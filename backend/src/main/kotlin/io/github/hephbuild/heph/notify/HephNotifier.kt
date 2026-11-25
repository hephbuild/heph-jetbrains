package io.github.hephbuild.heph.notify

import io.github.hephbuild.heph.HephBundle
import io.github.hephbuild.heph.core.HephBinaryMessages
import io.github.hephbuild.heph.core.HephBinaryResult
import io.github.hephbuild.heph.settings.HephConfigurable
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentHashMap

/** Notifications are de-duplicated per project per session. */
@Service(Service.Level.PROJECT)
class HephNotifier(private val project: Project) : Disposable {
    private val shown = ConcurrentHashMap.newKeySet<String>()

    fun binaryProblem(result: HephBinaryResult) {
        when (result) {
            is HephBinaryResult.Found -> Unit
            is HephBinaryResult.Failure -> post(
                HephBinaryMessages.key(result),
                HephBinaryMessages.title(result),
                HephBinaryMessages.content(result),
                null,
            )
        }
    }

    /** Re-arms every notification, e.g. after a restart or a healthy start. */
    fun reset() {
        shown.clear()
    }

    /** [restart] runs from the notification's Restart action. */
    fun serverCrashed(restart: () -> Unit) {
        post(
            CRASHED_KEY,
            HephBundle.message("notification.server.crashed.title"),
            HephBundle.message("notification.server.crashed.content"),
            restart,
        )
    }

    private fun post(key: String, title: String, content: String, restart: (() -> Unit)?) {
        if (!shown.add(key)) return
        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup(GROUP_ID)
            .createNotification(title, content, NotificationType.ERROR)
        if (restart != null) {
            notification.addAction(
                NotificationAction.createSimple(HephBundle.message("notification.action.restart")) { restart() },
            )
        }
        notification.addAction(
            NotificationAction.createSimple(HephBundle.message("notification.action.openSettings")) {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, HephConfigurable::class.java)
            },
        )
        notification.notify(project)
    }

    override fun dispose() = Unit

    companion object {
        const val GROUP_ID = "Heph"
        private const val CRASHED_KEY = "crashed"
    }
}
