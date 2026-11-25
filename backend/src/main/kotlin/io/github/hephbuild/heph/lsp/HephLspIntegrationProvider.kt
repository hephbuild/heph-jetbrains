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
import com.intellij.platform.lsp.api.LspIntegrationProvider
import com.intellij.util.EnvironmentUtil
import java.util.concurrent.TimeUnit

class HephLspIntegrationProvider : LspIntegrationProvider {
    var resolver: HephBinaryResolver = HephBinaryResolver()

    var nanoTime: () -> Long = System::nanoTime
    var pathValue: () -> String? = { EnvironmentUtil.getValue("PATH") }

    @Volatile
    private var cached: CachedBinary? = null

    private class CachedBinary(
        val configured: String,
        val path: String?,
        val at: Long,
        val result: HephBinaryResult.Found,
    )

    /** Heph may download and run a version pinned in `.hephconfig`, so never start it in untrusted projects. */
    var isTrusted: (Project) -> Boolean = { TrustedProjects.isProjectTrusted(it) }

    override fun fileOpened(
        project: Project,
        file: VirtualFile,
        clientStarter: LspIntegrationProvider.LspClientStarter,
    ) {
        if (!isTrusted(project)) return
        val settings = HephSettings.getInstance()
        val root = HephBuildFiles.rootFor(file) ?: return

        when (val result = resolve(settings.binaryPath)) {
            is HephBinaryResult.Found ->
                clientStarter.ensureClientStarted(HephLspClientDescriptor(project, root, result.path))
            else -> project.service<HephNotifier>().binaryProblem(result)
        }
    }

    /**
     * Runs inside a read action for every opened heph file, and each lookup stats every PATH entry, so a successful
     * one is reused briefly. Failures are never cached: a fixed setup is picked up at once.
     */
    private fun resolve(configured: String): HephBinaryResult {
        val path = pathValue()
        val now = nanoTime()
        cached?.let {
            if (it.configured == configured && it.path == path && now - it.at < CACHE_NANOS) return it.result
        }
        return resolver.resolve(configured).also {
            if (it is HephBinaryResult.Found) cached = CachedBinary(configured, path, now, it)
        }
    }

    private companion object {
        val CACHE_NANOS = TimeUnit.SECONDS.toNanos(5)
    }
}
