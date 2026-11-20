package io.github.hephbuild.heph.settings

import io.github.hephbuild.heph.HephBundle
import io.github.hephbuild.heph.filetype.HephFileTypeRefresher
import io.github.hephbuild.heph.core.HephBinaryResolver
import io.github.hephbuild.heph.core.HephBinaryResult
import io.github.hephbuild.heph.lsp.HephLspRestarter
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel

class HephConfigurable : BoundConfigurable(HephBundle.message("settings.title")) {
    override fun createPanel(): DialogPanel {
        val settings = HephSettings.getInstance()
        val detected = when (val result = HephBinaryResolver().resolve("")) {
            is HephBinaryResult.Found -> HephBundle.message("settings.binary.detected", result.path)
            else -> HephBundle.message("settings.binary.notDetected")
        }
        return panel {
            row(HephBundle.message("settings.binary")) {
                textFieldWithBrowseButton(
                    FileChooserDescriptorFactory.singleFile()
                        .withTitle(HephBundle.message("settings.binary.browse")),
                )
                    .bindText(settings::binaryPath)
                    .align(AlignX.FILL)
                    .comment("${HephBundle.message("settings.binary.comment")} $detected")
            }
            row(HephBundle.message("settings.args")) {
                textField()
                    .bindText(settings::serverArgs)
                    .align(AlignX.FILL)
                    .comment(HephBundle.message("settings.args.comment"))
            }
            row(HephBundle.message("settings.patterns")) {
                textField()
                    .bindText(settings::extraFilePatterns)
                    .align(AlignX.FILL)
                    .comment(HephBundle.message("settings.patterns.comment"))
            }
        }
    }

    override fun apply() {
        val oldPatterns = HephSettings.getInstance().extraFilePatterns
        super.apply()
        if (HephSettings.getInstance().extraFilePatterns != oldPatterns) {
            // Apply runs on the EDT (modal dialog is fine for a write action): retype open files before servers restart.
            if (ApplicationManager.getApplication().isDispatchThread) HephFileTypeRefresher.refreshNow() else HephFileTypeRefresher.refresh()
        }
        HephLspRestarter.restartAll()
    }
}
