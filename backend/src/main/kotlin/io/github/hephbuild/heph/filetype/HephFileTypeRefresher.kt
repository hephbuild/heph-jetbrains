package io.github.hephbuild.heph.filetype

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.fileTypes.ex.FileTypeManagerEx
import java.util.concurrent.atomic.AtomicBoolean

/** Forces file type re-detection after file name settings or `.hephconfig` files change. */
object HephFileTypeRefresher {
    private val pending = AtomicBoolean()

    /** Coalesced: many requests before the EDT runs cause one refresh. */
    fun refresh() {
        if (!pending.compareAndSet(false, true)) return
        ApplicationManager.getApplication().invokeLater({
            pending.set(false)
            refreshNow()
        }, ModalityState.nonModal())
    }

    /** Suspends until the EDT write action ran; waits out modal dialogs. */
    suspend fun refreshSuspending() {
        edtWriteAction { FileTypeManagerEx.getInstanceEx().makeFileTypesChange("Heph file names changed") {} }
    }

    /** Must run on the EDT. */
    fun refreshNow() {
        ApplicationManager.getApplication().runWriteAction {
            FileTypeManagerEx.getInstanceEx().makeFileTypesChange("Heph file names changed") {}
        }
    }
}
