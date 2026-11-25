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

    private data class LoadCase(val stored: String, val found: Path?, val want: HephSettingsPageDto)

    @Test
    fun load() {
        fun dto(path: String) = HephSettingsDto(path, "tool build-lsp", "")
        val cases = mapOf(
            "blank and detected reports the path and stays blank" to
                LoadCase("", detected, HephSettingsPageDto(dto(""), detected = detected.toString())),
            "whitespace counts as blank" to
                LoadCase("  ", detected, HephSettingsPageDto(dto("  "), detected = detected.toString())),
            "configured is kept and not resolved" to LoadCase("/opt/heph", detected, HephSettingsPageDto(dto("/opt/heph"))),
            "blank and undetected reports searched" to
                LoadCase("", null, HephSettingsPageDto(dto(""), searched = "PATH")),
            "configured and undetected has nothing extra" to
                LoadCase("/opt/heph", null, HephSettingsPageDto(dto("/opt/heph"))),
        )
        for ((name, case) in cases) {
            val fixture = Fixture(case.stored, resolver(case.found))
            fixture.settings.serverArgs = "tool build-lsp"
            assertEquals(name, case.want, fixture.controller.load())
            assertEquals("$name: nothing is persisted", case.stored, fixture.settings.binaryPath)
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

            assertEquals(name, HephSettingsPageDto(dto), page)
            assertEquals(name, dto.binaryPath, fixture.settings.binaryPath)
            assertEquals(name, dto.serverArgs, fixture.settings.serverArgs)
            assertEquals(name, dto.extraFilePatterns, fixture.settings.extraFilePatterns)
            assertEquals(name, listOf(case.wantFlag), fixture.flags)
        }
    }

    @Test
    fun blankSaveStaysBlankAndReportsDetection() {
        val fixture = Fixture("/opt/heph", resolver(detected))
        val page = fixture.controller.save(HephSettingsDto("", "tool build-lsp", ""))
        assertEquals("", page.settings.binaryPath)
        assertEquals("", fixture.settings.binaryPath)
        assertEquals(detected.toString(), page.detected)
        assertEquals(null, page.searched)
    }
}
