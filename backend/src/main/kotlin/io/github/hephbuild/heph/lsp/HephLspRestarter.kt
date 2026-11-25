package io.github.hephbuild.heph.lsp

import io.github.hephbuild.heph.notify.HephNotifier
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.platform.lsp.api.LspClientManager

object HephLspRestarter {
    fun restart(project: Project) {
        project.service<HephNotifier>().reset()
        LspClientManager.getInstance(project).stopAndRestartClientsIfNeeded(HephLspIntegrationProvider::class.java)
    }

    fun restartAll() {
        ProjectManager.getInstance().openProjects.filterNot { it.isDisposed }.forEach(::restart)
    }
}
