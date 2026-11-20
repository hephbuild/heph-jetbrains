package io.github.hephbuild.heph.core

import com.intellij.openapi.vfs.InvalidVirtualFileAccessException
import com.intellij.openapi.vfs.VirtualFile

object HephRootFinder {
    const val MARKER = ".hephconfig"

    /**
     * Nearest ancestor directory (or the directory itself) containing `.hephconfig`.
     * Null for invalid files: this runs from file type detection and indexing, which can race with deletion.
     */
    fun find(file: VirtualFile): VirtualFile? {
        try {
            var dir: VirtualFile? = if (file.isDirectory) file else file.parent
            while (dir != null) {
                if (!dir.isValid) return null
                if (dir.findChild(MARKER)?.isDirectory == false) return dir
                dir = dir.parent
            }
        } catch (_: InvalidVirtualFileAccessException) {
            return null
        }
        return null
    }
}
