package io.github.hephbuild.heph.lsp

import io.github.hephbuild.heph.settings.HephSettings
import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.platform.lsp.api.LspServerDescriptor
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class HephLspServerSupportProviderTest : BasePlatformTestCase() {
    override fun tearDown() {
        try {
            HephSettings.getInstance().binaryPath = ""
        } finally {
            super.tearDown()
        }
    }

    fun testMissingBinaryNotifiesOnceAndStartsNothing() {
        val notifications = mutableListOf<Notification>()
        project.messageBus.connect(testRootDisposable).subscribe(
            Notifications.TOPIC,
            object : Notifications {
                override fun notify(notification: Notification) {
                    notifications += notification
                }
            },
        )
        var started = 0
        val starter = object : LspServerSupportProvider.LspServerStarter {
            override fun ensureServerStarted(descriptor: LspServerDescriptor) {
                started++
            }
        }
        HephSettings.getInstance().binaryPath = "/nonexistent/heph"
        val fixture = myFixture.tempDirFixture
        fixture.createFile("repo/.hephconfig")
        val build = fixture.createFile("repo/BUILD")

        val provider = HephLspServerSupportProvider()
        provider.fileOpened(project, build, starter)
        provider.fileOpened(project, build, starter)

        assertEquals(0, started)
        assertEquals(1, notifications.size)
    }

    fun testUntrustedProjectStartsNothingAndStaysSilent() {
        val notifications = mutableListOf<Notification>()
        project.messageBus.connect(testRootDisposable).subscribe(
            Notifications.TOPIC,
            object : Notifications {
                override fun notify(notification: Notification) {
                    notifications += notification
                }
            },
        )
        var started = 0
        val starter = object : LspServerSupportProvider.LspServerStarter {
            override fun ensureServerStarted(descriptor: LspServerDescriptor) {
                started++
            }
        }
        val fixture = myFixture.tempDirFixture
        fixture.createFile("repo/.hephconfig")
        val build = fixture.createFile("repo/BUILD")
        val bin = java.nio.file.Files.createTempFile("heph", "").toFile().apply {
            setExecutable(true)
            deleteOnExit()
        }
        HephSettings.getInstance().binaryPath = bin.path

        val provider = HephLspServerSupportProvider()
        provider.isTrusted = { false }
        provider.fileOpened(project, build, starter)
        assertEquals(0, started)
        assertTrue(notifications.isEmpty())

        provider.isTrusted = { true }
        provider.fileOpened(project, build, starter)
        assertEquals(1, started)
    }
}
