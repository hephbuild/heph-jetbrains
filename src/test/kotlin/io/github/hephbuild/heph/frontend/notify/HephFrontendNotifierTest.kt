package io.github.hephbuild.heph.frontend.notify

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.github.hephbuild.heph.rpc.HephNotificationDto

class HephFrontendNotifierTest : BasePlatformTestCase() {
    private val posted = mutableListOf<Notification>()

    override fun setUp() {
        super.setUp()
        project.messageBus.connect(testRootDisposable).subscribe(
            Notifications.TOPIC,
            object : Notifications {
                override fun notify(notification: Notification) {
                    posted += notification
                }
            },
        )
    }

    fun testShowPostsErrorInHephGroupWithActions() {
        val cases = mapOf(
            "restartable" to (true to listOf("Restart Server", "Open Settings")),
            "not restartable" to (false to listOf("Open Settings")),
        )
        for ((name, case) in cases) {
            posted.clear()
            project.service<HephFrontendNotifier>().show(HephNotificationDto("Title", "Content", case.first))
            val notification = posted.single()
            assertEquals(name, "Heph", notification.groupId)
            assertEquals(name, "Title", notification.title)
            assertEquals(name, "Content", notification.content)
            assertEquals(name, NotificationType.ERROR, notification.type)
            assertEquals(name, case.second, notification.actions.map { it.templateText })
            notification.expire()
        }
    }
}
