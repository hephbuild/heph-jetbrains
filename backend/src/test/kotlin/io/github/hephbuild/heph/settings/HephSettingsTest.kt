package io.github.hephbuild.heph.settings

import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.State
import com.intellij.util.xmlb.XmlSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HephSettingsTest {
    @Test
    fun defaults() {
        val settings = HephSettings()
        assertEquals("", settings.binaryPath)
        assertEquals("tool build-lsp", settings.serverArgs)
        assertEquals("", settings.extraFilePatterns)
    }

    @Test
    fun storageDoesNotRoam() {
        val storage = HephSettings::class.java.getAnnotation(State::class.java).storages.single()
        assertEquals("heph.xml", storage.value)
        assertEquals(RoamingType.DISABLED, storage.roamingType)
    }

    @Test
    fun stateRoundTrip() {
        val state = HephSettings.State().apply {
            binaryPath = "/opt/heph"
            serverArgs = "tool build-lsp --debug"
            extraFilePatterns = "BUILD2;*.heph"
        }
        val element = XmlSerializer.serialize(state)
        val loaded = HephSettings()
        loaded.loadState(XmlSerializer.deserialize(element, HephSettings.State::class.java))
        assertEquals("/opt/heph", loaded.binaryPath)
        assertEquals("tool build-lsp --debug", loaded.serverArgs)
        assertEquals("BUILD2;*.heph", loaded.extraFilePatterns)
    }

    @Test
    fun emptyServerArgsPersist() {
        val state = HephSettings.State().apply { serverArgs = "" }
        val loaded = HephSettings()
        loaded.loadState(XmlSerializer.deserialize(XmlSerializer.serialize(state), HephSettings.State::class.java))
        assertEquals("", loaded.serverArgs)

        val settings = HephSettings()
        settings.serverArgs = ""
        assertEquals("", settings.serverArgs)
    }

    @Test
    fun fileMatcherFollowsPatternsAndIsCached() {
        val settings = HephSettings()
        val first = settings.fileMatcher
        assertSame(first, settings.fileMatcher)
        assertFalse(first.matches("BUILD2"))

        settings.extraFilePatterns = "BUILD2"
        val second = settings.fileMatcher
        assertNotSame(first, second)
        assertTrue(second.matches("BUILD2"))
        assertSame(second, settings.fileMatcher)

        settings.extraFilePatterns = ""
        assertFalse(settings.fileMatcher.matches("BUILD2"))
    }

    @Test
    fun seedBinaryPath() {
        val cases = mapOf(
            "blank current, detected" to (("" to "/usr/local/bin/heph") to "/usr/local/bin/heph"),
            "whitespace current, detected" to (("  " to "/usr/local/bin/heph") to "/usr/local/bin/heph"),
            "blank current, nothing detected" to (("" to null) to ""),
            "whitespace current, nothing detected" to (("  " to null) to "  "),
            "set current, detected" to (("/opt/heph" to "/usr/local/bin/heph") to "/opt/heph"),
            "set current, nothing detected" to (("/opt/heph" to null) to "/opt/heph"),
        )
        for ((name, case) in cases) {
            assertEquals(name, case.second, HephSettings.seedBinaryPath(case.first.first, case.first.second))
        }
    }
}
