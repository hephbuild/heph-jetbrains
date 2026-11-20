package io.github.hephbuild.heph.notify

import io.github.hephbuild.heph.core.HephBinaryResult
import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class HephNotifierTest : BasePlatformTestCase() {
    fun testCrashNotifiesAgainAfterReset() {
        val notifications = mutableListOf<Notification>()
        project.messageBus.connect(testRootDisposable).subscribe(
            Notifications.TOPIC,
            object : Notifications {
                override fun notify(notification: Notification) {
                    notifications += notification
                }
            },
        )
        val notifier = project.service<HephNotifier>()

        notifier.serverCrashed {}
        notifier.serverCrashed {}
        assertEquals(1, notifications.size)

        notifier.reset()
        notifier.serverCrashed {}
        assertEquals(2, notifications.size)

        notifier.binaryProblem(HephBinaryResult.NotFound(null))
        notifier.binaryProblem(HephBinaryResult.NotFound(null))
        assertEquals(3, notifications.size)

        notifier.reset()
        notifier.binaryProblem(HephBinaryResult.NotFound(null))
        assertEquals(4, notifications.size)
    }
}
