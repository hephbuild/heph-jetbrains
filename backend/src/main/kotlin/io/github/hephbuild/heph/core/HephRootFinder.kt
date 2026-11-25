package io.github.hephbuild.heph.core

import com.intellij.openapi.vfs.InvalidVirtualFileAccessException
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

object HephRootFinder {
    const val MARKER = ".hephconfig"

    private const val MAX_CACHED_DIRS = 10_000

    private class Entry(val stamp: Long, val root: Optional<VirtualFile>)

    /** Per starting directory; an entry is valid only for the VFS structure it was computed against. */
    private val cache = ConcurrentHashMap<VirtualFile, Entry>()

    /**
     * Nearest ancestor directory (or the directory itself) containing `.hephconfig`.
     * Null for invalid files: this runs from file type detection and indexing, which can race with deletion.
     * Walks up on every call for a BUILD-named file outside any heph repo (e.g. Bazel), so results are reused until
     * the VFS structure changes, which includes a `.hephconfig` being created, deleted, moved or renamed.
     */
    fun find(file: VirtualFile): VirtualFile? {
        try {
            val start = (if (file.isDirectory) file else file.parent) ?: return null
            val stamp = VirtualFileManager.getInstance().structureModificationCount
            cache[start]?.let { if (it.stamp == stamp) return it.root.orElse(null) }
            val root = walk(start)
            // An invalid start gives no root only because it is gone, not because there is none.
            if (start.isValid && VirtualFileManager.getInstance().structureModificationCount == stamp) {
                if (cache.size >= MAX_CACHED_DIRS) cache.clear()
                cache[start] = Entry(stamp, Optional.ofNullable(root))
            }
            return root
        } catch (_: InvalidVirtualFileAccessException) {
            return null
        }
    }

    private fun walk(start: VirtualFile): VirtualFile? {
        var dir: VirtualFile? = start
        while (dir != null) {
            if (!dir.isValid) return null
            if (dir.findChild(MARKER)?.isDirectory == false) return dir
            dir = dir.parent
        }
        return null
    }
}
