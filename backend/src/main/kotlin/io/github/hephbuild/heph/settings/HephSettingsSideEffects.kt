package io.github.hephbuild.heph.settings

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import io.github.hephbuild.heph.filetype.HephFileTypeRefresher
import io.github.hephbuild.heph.lsp.HephLspRestarter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object HephSettingsSideEffects {
    private val LOG = logger<HephSettingsSideEffects>()

    /**
     * Fire-and-forget: waiting on the EDT from a save under a modal Settings dialog would deadlock.
     * Both steps wait for the dialog to close.
     */
    fun schedule(patternsChanged: Boolean) {
        service<HephBackendScope>().scope.launch {
            if (patternsChanged) {
                try {
                    HephFileTypeRefresher.refreshSuspending()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    LOG.warn("Heph file type refresh failed", e)
                }
            }
            withContext(Dispatchers.EDT) { HephLspRestarter.restartAll() }
        }
    }
}
