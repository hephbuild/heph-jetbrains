package io.github.hephbuild.heph.lsp

import io.github.hephbuild.heph.core.HephBinaryResolver
import io.github.hephbuild.heph.core.HephBinaryResult
import io.github.hephbuild.heph.core.HephBuildFiles
import io.github.hephbuild.heph.notify.HephNotifier
import io.github.hephbuild.heph.settings.HephSettings
import com.intellij.ide.trustedProjects.TrustedProjects
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerSupportProvider

class HephLspServerSupportProvider : LspServerSupportProvider {
    var resolver: HephBinaryResolver = HephBinaryResolver()

    /** Heph may download and run a version pinned in `.hephconfig`, so never start it in untrusted projects. */
    var isTrusted: (Project) -> Boolean = { TrustedProjects.isProjectTrusted(it) }

    override fun fileOpened(
        project: Project,
        file: VirtualFile,
        serverStarter: LspServerSupportProvider.LspServerStarter,
    ) {
        if (!isTrusted(project)) return
        val settings = HephSettings.getInstance()
        val root = HephBuildFiles.rootFor(file) ?: return

        when (val result = resolver.resolve(settings.binaryPath)) {
            is HephBinaryResult.Found ->
                serverStarter.ensureServerStarted(HephLspServerDescriptor(project, root, result.path))
            else -> project.service<HephNotifier>().binaryProblem(result)
        }
    }
}
