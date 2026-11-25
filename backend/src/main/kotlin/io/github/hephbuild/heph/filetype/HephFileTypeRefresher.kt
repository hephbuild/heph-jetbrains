package io.github.hephbuild.heph.filetype

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.fileTypes.ex.FileTypeManagerEx
import io.github.hephbuild.heph.lsp.HephLspRestarter
import java.util.concurrent.atomic.AtomicBoolean

/** Forces file type re-detection after file name settings or `.hephconfig` files change. */
object HephFileTypeRefresher {
    private val pending = AtomicBoolean()

    /**
     * Runs after [refresh]. The platform starts a language server only when a file is opened, never on a file type
     * change, so open files whose type changed need this nudge: it starts servers for new heph files and stops
     * those of files that no longer are.
     */
    @Volatile
    var afterRefresh: () -> Unit = HephLspRestarter::restartAll

    /** Coalesced: many requests before the EDT runs cause one refresh. */
    fun refresh() {
        if (!pending.compareAndSet(false, true)) return
        ApplicationManager.getApplication().invokeLater({
            pending.set(false)
            refreshNow()
            afterRefresh()
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
