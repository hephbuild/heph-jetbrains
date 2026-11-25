package io.github.hephbuild.heph.rpc

import com.intellij.openapi.application.EDT
import com.intellij.platform.project.ProjectId
import com.intellij.platform.project.findProjectOrNull
import io.github.hephbuild.heph.lsp.HephLspRestarter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.withContext

class HephNotificationsApiImpl : HephNotificationsApi {
    // Backed by the notifier's event channel once the frontend posts notifications.
    override suspend fun notifications(projectId: ProjectId): Flow<HephNotificationDto> = emptyFlow()

    override suspend fun restartServer(projectId: ProjectId) {
        val project = projectId.findProjectOrNull() ?: return
        withContext(Dispatchers.EDT) { HephLspRestarter.restart(project) }
    }
}
