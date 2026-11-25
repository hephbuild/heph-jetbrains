package io.github.hephbuild.heph.notify

import io.github.hephbuild.heph.core.HephBinaryResult
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.github.hephbuild.heph.rpc.HephNotificationDto

class HephNotifierTest : BasePlatformTestCase() {
    fun testCrashNotifiesAgainAfterReset() {
        val notifier = project.service<HephNotifier>()
        notifier.reset()
        notifier.drain()
        val received = mutableListOf<HephNotificationDto>()

        notifier.serverCrashed()
        notifier.serverCrashed()
        received += notifier.drain()
        assertEquals(1, received.size)
        assertTrue("a crash is restartable", received[0].restartable)

        notifier.reset()
        notifier.serverCrashed()
        received += notifier.drain()
        assertEquals(2, received.size)

        val autoDetect = HephBinaryResult.NotFound.AutoDetect(listOf("PATH", "~/.local/bin"))
        notifier.binaryProblem(autoDetect)
        notifier.binaryProblem(autoDetect)
        received += notifier.drain()
        assertEquals(3, received.size)
        assertFalse("a binary problem is not restartable", received[2].restartable)

        notifier.reset()
        notifier.binaryProblem(autoDetect)
        received += notifier.drain()
        assertEquals(4, received.size)
    }

    fun testBinaryProblemsAreDeduplicatedByKindAndValue() {
        val notifier = project.service<HephNotifier>()
        notifier.reset()
        notifier.drain()
        val received = mutableListOf<HephNotificationDto>()
        val problems = listOf(
            HephBinaryResult.NotFound.AutoDetect(listOf("PATH", "~/.local/bin")),
            HephBinaryResult.NotFound.Configured("/x/heph"),
            HephBinaryResult.NotAFile("/x/dir"),
            HephBinaryResult.NotExecutable("/x/noexec"),
            HephBinaryResult.Inaccessible("/x/locked"),
        )
        for (problem in problems) notifier.binaryProblem(problem)
        for (problem in problems) notifier.binaryProblem(problem)
        received += notifier.drain()
        assertEquals("same kind and value is suppressed", problems.size, received.size)

        val differentValues = listOf(
            HephBinaryResult.NotFound.Configured("/y/heph"),
            HephBinaryResult.NotAFile("/y/dir"),
            HephBinaryResult.NotExecutable("/y/noexec"),
            HephBinaryResult.Inaccessible("/y/locked"),
        )
        for (problem in differentValues) notifier.binaryProblem(problem)
        received += notifier.drain()
        assertEquals("same kind, different value posts again", problems.size + differentValues.size, received.size)

        notifier.binaryProblem(HephBinaryResult.NotFound.AutoDetect(listOf("PATH")))
        received += notifier.drain()
        assertEquals("auto-detect has one key per kind", problems.size + differentValues.size, received.size)

        notifier.binaryProblem(HephBinaryResult.Found(java.nio.file.Path.of("/x/heph")))
        received += notifier.drain()
        assertEquals("found posts nothing", problems.size + differentValues.size, received.size)

        val text = received.joinToString("\n") { it.title + " | " + it.content }
        assertTrue(text, text.contains("Searched: PATH, ~/.local/bin"))
        assertTrue(text, text.contains("/x/dir is not a file"))
        assertTrue(text, text.contains("/y/noexec is not executable"))
        assertTrue(text, text.contains("Cannot access /x/locked"))
        assertTrue(text, text.contains("not found: /x/heph"))
        assertFalse(text, text.contains("heph1"))
        assertTrue("no binary problem is restartable", received.none { it.restartable })

        notifier.reset()
        for (problem in problems + differentValues) notifier.binaryProblem(problem)
        received += notifier.drain()
        assertEquals("reset re-arms every key", 2 * (problems.size + differentValues.size), received.size)
    }

    fun testOnlyTheNewestSixteenEventsAreKept() {
        val notifier = project.service<HephNotifier>()
        notifier.reset()
        notifier.drain()
        for (i in 1..20) {
            notifier.reset()
            notifier.binaryProblem(HephBinaryResult.NotFound.Configured("/x/heph$i"))
        }
        val received = notifier.drain()
        assertEquals(16, received.size)
        assertTrue(received.first().content, received.first().content.endsWith("/x/heph5"))
        assertTrue(received.last().content, received.last().content.endsWith("/x/heph20"))
    }
}
