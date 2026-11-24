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

        val autoDetect = HephBinaryResult.NotFound.AutoDetect(listOf("PATH", "~/.local/bin"))
        notifier.binaryProblem(autoDetect)
        notifier.binaryProblem(autoDetect)
        assertEquals(3, notifications.size)

        notifier.reset()
        notifier.binaryProblem(autoDetect)
        assertEquals(4, notifications.size)
    }

    fun testBinaryProblemsAreDeduplicatedByKindAndValue() {
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
        val problems = listOf(
            HephBinaryResult.NotFound.AutoDetect(listOf("PATH", "~/.local/bin")),
            HephBinaryResult.NotFound.Configured("/x/heph"),
            HephBinaryResult.NotAFile("/x/dir"),
            HephBinaryResult.NotExecutable("/x/noexec"),
            HephBinaryResult.Inaccessible("/x/locked"),
        )
        for (problem in problems) notifier.binaryProblem(problem)
        for (problem in problems) notifier.binaryProblem(problem)
        assertEquals("same kind and value is suppressed", problems.size, notifications.size)

        val differentValues = listOf(
            HephBinaryResult.NotFound.Configured("/y/heph"),
            HephBinaryResult.NotAFile("/y/dir"),
            HephBinaryResult.NotExecutable("/y/noexec"),
            HephBinaryResult.Inaccessible("/y/locked"),
        )
        for (problem in differentValues) notifier.binaryProblem(problem)
        assertEquals("same kind, different value posts again", problems.size + differentValues.size, notifications.size)

        notifier.binaryProblem(HephBinaryResult.NotFound.AutoDetect(listOf("PATH")))
        assertEquals("auto-detect has one key per kind", problems.size + differentValues.size, notifications.size)

        notifier.binaryProblem(HephBinaryResult.Found(java.nio.file.Path.of("/x/heph")))
        assertEquals("found posts nothing", problems.size + differentValues.size, notifications.size)

        val text = notifications.joinToString("\n") { it.title + " | " + it.content }
        assertTrue(text, text.contains("Searched: PATH, ~/.local/bin"))
        assertTrue(text, text.contains("/x/dir is not a file"))
        assertTrue(text, text.contains("/y/noexec is not executable"))
        assertTrue(text, text.contains("Cannot access /x/locked"))
        assertTrue(text, text.contains("not found: /x/heph"))
        assertFalse(text, text.contains("heph1"))

        notifier.reset()
        for (problem in problems + differentValues) notifier.binaryProblem(problem)
        assertEquals("reset re-arms every key", 2 * (problems.size + differentValues.size), notifications.size)
    }
}
