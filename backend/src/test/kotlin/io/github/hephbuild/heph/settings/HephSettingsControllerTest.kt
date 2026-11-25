package io.github.hephbuild.heph.settings

import io.github.hephbuild.heph.core.HephBinaryResolver
import io.github.hephbuild.heph.rpc.HephSettingsDto
import io.github.hephbuild.heph.rpc.HephSettingsPageDto
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Path

class HephSettingsControllerTest {
    private val detected = Path.of("/usr/local/bin/heph")

    private fun resolver(found: Path?) =
        HephBinaryResolver(findOnPath = { found }, fallbackDirs = emptyList(), home = null)

    private class Fixture(binaryPath: String = "", resolver: HephBinaryResolver) {
        val settings = HephSettings().apply { this.binaryPath = binaryPath }
        val flags = mutableListOf<Boolean>()
        val controller = HephSettingsController(settings, resolver) { flags += it }
    }

    private data class LoadCase(val stored: String, val found: Path?, val wantPath: String, val wantSearched: String?)

    @Test
    fun load() {
        val cases = mapOf(
            "blank and detected seeds and persists" to LoadCase("", detected, detected.toString(), null),
            "configured is kept" to LoadCase("/opt/heph", detected, "/opt/heph", null),
            "blank and undetected reports searched" to LoadCase("", null, "", "PATH"),
            "configured and undetected has no searched" to LoadCase("/opt/heph", null, "/opt/heph", null),
        )
        for ((name, case) in cases) {
            val fixture = Fixture(case.stored, resolver(case.found))
            val page = fixture.controller.load()
            assertEquals(name, case.wantPath, page.settings.binaryPath)
            assertEquals(name, case.wantPath, fixture.settings.binaryPath)
            assertEquals(name, case.wantSearched, page.searched)
            assertEquals(name, emptyList<Boolean>(), fixture.flags)
        }
    }

    private data class SaveCase(val oldPatterns: String, val newPatterns: String, val wantFlag: Boolean)

    @Test
    fun saveWritesAllFieldsAndFlagsPatternChange() {
        val cases = mapOf(
            "patterns unchanged" to SaveCase("BUILD2", "BUILD2", false),
            "patterns added" to SaveCase("", "BUILD2", true),
            "patterns removed" to SaveCase("BUILD2", "", true),
        )
        for ((name, case) in cases) {
            val fixture = Fixture(resolver = resolver(detected))
            fixture.settings.extraFilePatterns = case.oldPatterns
            val dto = HephSettingsDto(" /opt/heph ", "tool build-lsp --debug", case.newPatterns)

            val page = fixture.controller.save(dto)

            assertEquals(name, HephSettingsPageDto(dto, null), page)
            assertEquals(name, dto.binaryPath, fixture.settings.binaryPath)
            assertEquals(name, dto.serverArgs, fixture.settings.serverArgs)
            assertEquals(name, dto.extraFilePatterns, fixture.settings.extraFilePatterns)
            assertEquals(name, listOf(case.wantFlag), fixture.flags)
        }
    }

    @Test
    fun blankSaveDoesNotSeed() {
        val fixture = Fixture("/opt/heph", resolver(detected))
        val page = fixture.controller.save(HephSettingsDto("", "tool build-lsp", ""))
        assertEquals("", page.settings.binaryPath)
        assertEquals("", fixture.settings.binaryPath)
        assertEquals(null, page.searched)
    }
}
