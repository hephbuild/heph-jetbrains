package io.github.hephbuild.heph.filetype

import com.intellij.openapi.vfs.InvalidVirtualFileAccessException
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import io.github.hephbuild.heph.core.HephRootFinder

/** Creating, deleting, renaming, moving or copying a `.hephconfig` changes which BUILD files are heph files. */
class HephConfigFileListener : BulkFileListener {
    override fun after(events: List<VFileEvent>) {
        if (events.any(::isMarkerEvent)) HephFileTypeRefresher.refresh()
    }

    companion object {
        /** A directory added or moved with a `.hephconfig` directly inside (e.g. a checkout); no recursion. */
        private fun directoryHasMarker(dir: VirtualFile?): Boolean =
            try {
                dir != null && dir.isValid && dir.isDirectory && dir.findChild(HephRootFinder.MARKER) != null
            } catch (_: InvalidVirtualFileAccessException) {
                false
            }

        /** Content changes and unrelated events are ignored. */
        fun isMarkerEvent(event: VFileEvent): Boolean {
            val marker = HephRootFinder.MARKER
            return when (event) {
                is VFileCreateEvent -> event.childName == marker || (event.isDirectory && directoryHasMarker(event.file))
                is VFileDeleteEvent -> event.file.name == marker
                is VFileMoveEvent -> event.file.name == marker || directoryHasMarker(event.file)
                is VFileCopyEvent -> event.newChildName == marker || directoryHasMarker(event.findCreatedFile())
                is VFilePropertyChangeEvent -> event.isRename && (event.oldValue == marker || event.newValue == marker)
                else -> false
            }
        }
    }
}
