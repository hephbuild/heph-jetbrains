package io.github.hephbuild.heph.frontend.settings

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.observable.properties.AtomicBooleanProperty
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.platform.ide.productMode.IdeProductMode
import com.intellij.platform.ide.progress.ModalTaskOwner
import com.intellij.platform.ide.progress.TaskCancellation
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.platform.util.coroutines.childScope
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import io.github.hephbuild.heph.HephBundle
import io.github.hephbuild.heph.frontend.HephFrontendScope
import io.github.hephbuild.heph.rpc.HephSettingsApi
import io.github.hephbuild.heph.rpc.HephSettingsDto
import io.github.hephbuild.heph.rpc.HephSettingsPageDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Frontend page over the backend-owned settings; values load and save through [HephSettingsApi]. */
class HephConfigurable : BoundConfigurable(HephBundle.message("settings.title")) {
    private val form = HephSettingsForm()
    private val loaded = AtomicBooleanProperty(false)
    private var lastLoaded = HephSettingsDto("", "", "")
    private var loadScope: CoroutineScope? = null
    private var dialogPanel: DialogPanel? = null
    private var binaryCell: Cell<TextFieldWithBrowseButton>? = null
    private var failureCell: Cell<*>? = null

    override fun createPanel(): DialogPanel {
        val descriptor = FileChooserDescriptorFactory.singleFile()
            .withTitle(HephBundle.message("settings.binary.browse"))
        // A normal IDE keeps the native chooser; the client needs IDEA's so it can browse the host.
        descriptor.isForcedToUseIdeaFileChooser = IdeProductMode.isFrontend
        val result = panel {
            row {
                val failure = label(HephBundle.message("settings.loadFailed"))
                    .applyToComponent { foreground = JBColor.RED }
                    .visible(false)
                failureCell = failure
            }
            row(HephBundle.message("settings.binary")) {
                binaryCell = textFieldWithBrowseButton(descriptor)
                    .bindText(form::binaryPath)
                    .align(AlignX.FILL)
                    .enabledIf(loaded)
                    .comment(HephBundle.message("settings.binary.comment"))
            }
            row(HephBundle.message("settings.args")) {
                textField()
                    .bindText(form::serverArgs)
                    .align(AlignX.FILL)
                    .enabledIf(loaded)
                    .comment(HephBundle.message("settings.args.comment"))
            }
            row(HephBundle.message("settings.patterns")) {
                textField()
                    .bindText(form::extraFilePatterns)
                    .align(AlignX.FILL)
                    .enabledIf(loaded)
                    .comment(HephBundle.message("settings.patterns.comment"))
            }
        }
        dialogPanel = result
        startLoad()
        return result
    }

    private fun startLoad() {
        val scope = service<HephFrontendScope>().scope.childScope("HephSettingsPage")
        loadScope = scope
        // The page may open before its dialog is modal; the continuation only touches this panel, so any modality will do.
        scope.launch(ModalityState.any().asContextElement()) {
            val page = try {
                HephSettingsApi.getInstance().load()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LOG.warn("Failed to load Heph settings", e)
                withContext(Dispatchers.EDT) { failureCell?.visible(true) }
                return@launch
            }
            withContext(Dispatchers.EDT) {
                show(page)
                loaded.set(true)
            }
        }
    }

    private fun show(page: HephSettingsPageDto) {
        lastLoaded = page.settings
        form.set(page.settings)
        super.reset()
        binaryCell?.comment?.text = HephSettingsForm.binaryComment(page)
        (binaryCell?.component?.textField as? JBTextField)?.emptyText?.setText(HephSettingsForm.binaryPlaceholder(page))
    }

    override fun isModified(): Boolean = loaded.get() && super.isModified()

    override fun reset() {
        form.set(lastLoaded)
        super.reset()
    }

    override fun apply() {
        if (!loaded.get()) return
        super.apply()
        val settings = form.toDto()
        val owner = dialogPanel?.let { ModalTaskOwner.component(it) } ?: ModalTaskOwner.guess()
        val page = try {
            // Non-cancellable so a user cancel cannot throw out of apply().
            runWithModalProgressBlocking(owner, HephBundle.message("settings.saving"), TaskCancellation.nonCancellable()) {
                HephSettingsApi.getInstance().save(settings)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LOG.warn("Failed to save Heph settings", e)
            throw ConfigurationException(HephBundle.message("settings.saveFailed"))
        }
        show(page)
    }

    override fun disposeUIResources() {
        loadScope?.cancel()
        loadScope = null
        loaded.set(false)
        lastLoaded = HephSettingsDto("", "", "")
        dialogPanel = null
        binaryCell = null
        failureCell = null
        super.disposeUIResources()
    }

    private companion object {
        val LOG = logger<HephConfigurable>()
    }
}
