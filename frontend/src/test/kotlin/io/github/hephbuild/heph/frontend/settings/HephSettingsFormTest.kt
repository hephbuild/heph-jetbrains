package io.github.hephbuild.heph.frontend.settings

import io.github.hephbuild.heph.HephBundle
import io.github.hephbuild.heph.rpc.HephSettingsDto
import io.github.hephbuild.heph.rpc.HephSettingsPageDto
import org.junit.Assert.assertEquals
import org.junit.Test

class HephSettingsFormTest {
    @Test
    fun setAndToDtoRoundTrip() {
        val dto = HephSettingsDto("/opt/heph", "tool build-lsp", "BUILD2;*.heph")
        val form = HephSettingsForm()
        form.set(dto)
        assertEquals("/opt/heph", form.binaryPath)
        assertEquals("tool build-lsp", form.serverArgs)
        assertEquals("BUILD2;*.heph", form.extraFilePatterns)
        assertEquals(dto, form.toDto())
    }

    @Test
    fun toDtoReflectsEdits() {
        val form = HephSettingsForm()
        form.set(HephSettingsDto("/opt/heph", "a", "b"))
        form.binaryPath = " /other/heph "
        assertEquals(HephSettingsDto(" /other/heph ", "a", "b"), form.toDto())
    }

    @Test
    fun binaryComment() {
        val found = HephSettingsDto("/opt/heph", "", "")
        val blank = HephSettingsDto("", "", "")
        val plain = HephBundle.message("settings.binary.comment")
        val cases = mapOf(
            "configured path" to (HephSettingsPageDto(found, null) to plain),
            "blank and not detected" to (HephSettingsPageDto(blank, "PATH, ~/.local/bin") to
                HephBundle.message("settings.binary.notDetected", "PATH, ~/.local/bin")),
            "blank without searched (after save)" to (HephSettingsPageDto(blank, null) to plain),
            "searched is ignored once a path is set" to (HephSettingsPageDto(found, "PATH") to plain),
        )
        for ((name, case) in cases) {
            assertEquals(name, case.second, HephSettingsForm.binaryComment(case.first))
        }
    }
}
