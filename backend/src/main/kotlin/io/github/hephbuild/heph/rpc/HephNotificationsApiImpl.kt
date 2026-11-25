package io.github.hephbuild.heph.rpc

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.platform.project.ProjectId
import com.intellij.platform.project.findProjectOrNull
import io.github.hephbuild.heph.lsp.HephLspRestarter
import io.github.hephbuild.heph.notify.HephNotifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext

class HephNotificationsApiImpl : HephNotificationsApi {
    override suspend fun notifications(projectId: ProjectId): Flow<HephNotificationDto> {
        // Failing, not returning an empty flow, lets the frontend retry: it may subscribe before the project is known here.
        val project = projectId.findProjectOrNull() ?: error("No project for $projectId")
        return project.service<HephNotifier>().events.receiveAsFlow()
    }

    override suspend fun restartServer(projectId: ProjectId) {
        val project = projectId.findProjectOrNull() ?: return
        withContext(Dispatchers.EDT) { HephLspRestarter.restart(project) }
    }
}
