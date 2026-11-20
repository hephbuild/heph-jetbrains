package io.github.hephbuild.heph.lsp

import io.github.hephbuild.heph.core.HephCommandLine
import io.github.hephbuild.heph.core.HephBuildFiles
import io.github.hephbuild.heph.settings.HephSettings
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerDescriptor
import com.intellij.platform.lsp.api.LspServerListener
import com.intellij.platform.lsp.api.customization.LspCustomization
import io.github.hephbuild.heph.notify.HephNotifier
import org.eclipse.lsp4j.InitializeResult
import java.nio.file.Path

/** One server per heph root: the server handles a single workspace folder. */
class HephLspServerDescriptor(
    project: Project,
    private val root: VirtualFile,
    private val binary: Path,
) : LspServerDescriptor(project, "heph", root) {

    override fun isSupportedFile(file: VirtualFile): Boolean =
        VfsUtilCore.isAncestor(root, file, true) && HephBuildFiles.rootFor(file) == root

    override val lspServerListener: LspServerListener = object : LspServerListener {
        override fun serverInitialized(params: InitializeResult) {
            LOG.info("heph server initialized, root=${root.path}, formatting=${params.capabilities?.documentFormattingProvider}")
            project.service<HephNotifier>().reset()
        }

        override fun serverStopped(shutdownNormally: Boolean) {
            if (!shutdownNormally) {
                project.service<HephNotifier>().serverCrashed { HephLspRestarter.restart(project) }
            }
        }
    }

    override val lspCustomization: LspCustomization get() = CUSTOMIZATION

    override fun getLanguageId(file: VirtualFile): String = LANGUAGE_ID

    override fun createCommandLine(): GeneralCommandLine =
        HephCommandLine.build(binary, HephSettings.getInstance().serverArgs, root.toNioPath())

    override fun equals(other: Any?): Boolean =
        other is HephLspServerDescriptor && other.project == project && other.root == root

    override fun hashCode(): Int = 31 * project.hashCode() + root.hashCode()

    companion object {
        const val LANGUAGE_ID = "hephbuild"
        private val CUSTOMIZATION = HephLspCustomization()
        private val LOG = logger<HephLspServerDescriptor>()
    }
}
