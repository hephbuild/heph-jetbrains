package io.github.hephbuild.heph.lsp

import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.customization.LspCompletionCustomizer
import com.intellij.platform.lsp.api.customization.LspCustomization
import com.intellij.platform.lsp.api.customization.LspFormattingCustomizer
import com.intellij.platform.lsp.api.customization.LspFormattingSupport

/**
 * By default the platform lets the server format a file only when the IDE cannot AND the server registered formatting
 * dynamically for it. The heph server advertises a static `documentFormattingProvider`, so it must always format.
 */
class HephLspCustomization : LspCustomization() {
    override val formattingCustomizer: LspFormattingCustomizer = HephFormattingSupport()

    override val completionCustomizer: LspCompletionCustomizer = HephCompletionSupport()
}

class HephFormattingSupport : LspFormattingSupport() {
    override fun shouldFormatThisFileExclusivelyByServer(
        file: VirtualFile,
        ideCanFormatThisFileItself: Boolean,
        serverExplicitlyWantsToFormatThisFile: Boolean,
    ): Boolean {
        LOG.debug { "format ${file.path}: ideCanFormat=$ideCanFormatThisFileItself serverExplicit=$serverExplicitlyWantsToFormatThisFile -> server" }
        return true
    }

    private companion object {
        val LOG = logger<HephFormattingSupport>()
    }
}
