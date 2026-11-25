package io.github.hephbuild.heph.frontend.notify

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.platform.project.projectId
import fleet.rpc.client.durable
import io.github.hephbuild.heph.HephBundle
import io.github.hephbuild.heph.frontend.settings.HephConfigurable
import io.github.hephbuild.heph.rpc.HephNotificationDto
import io.github.hephbuild.heph.rpc.HephNotificationsApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/** Shows the backend's notifications as balloons in the frontend, where the settings page lives. */
@Service(Service.Level.PROJECT)
class HephFrontendNotifier(private val project: Project, private val scope: CoroutineScope) {
    private val subscribed = AtomicBoolean()

    /** Idempotent; collects until the project closes. */
    fun subscribe() {
        if (!subscribed.compareAndSet(false, true)) return
        scope.launch {
            try {
                durable {
                    HephNotificationsApi.getInstance().notifications(project.projectId()).collect { show(it) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LOG.warn("Heph notification subscription failed", e)
            }
        }
    }

    fun show(dto: HephNotificationDto): Notification {
        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup(GROUP_ID)
            .createNotification(dto.title, dto.content, NotificationType.ERROR)
        if (dto.restartable) {
            notification.addAction(
                NotificationAction.createSimple(HephBundle.message("notification.action.restart")) { restartServer() },
            )
        }
        notification.addAction(
            NotificationAction.createSimple(HephBundle.message("notification.action.openSettings")) {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, HephConfigurable::class.java)
            },
        )
        notification.notify(project)
        return notification
    }

    private fun restartServer() {
        scope.launch {
            try {
                HephNotificationsApi.getInstance().restartServer(project.projectId())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LOG.warn("Failed to restart the Heph server", e)
            }
        }
    }

    companion object {
        const val GROUP_ID = "Heph"
        private val LOG = logger<HephFrontendNotifier>()
    }
}
