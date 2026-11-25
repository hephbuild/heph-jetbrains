package io.github.hephbuild.heph.frontend.settings

import io.github.hephbuild.heph.HephBundle
import io.github.hephbuild.heph.rpc.HephSettingsDto
import io.github.hephbuild.heph.rpc.HephSettingsPageDto

/** Mutable state bound to the settings page fields. */
class HephSettingsForm {
    var binaryPath = ""
    var serverArgs = ""
    var extraFilePatterns = ""

    fun set(dto: HephSettingsDto) {
        binaryPath = dto.binaryPath
        serverArgs = dto.serverArgs
        extraFilePatterns = dto.extraFilePatterns
    }

    fun toDto() = HephSettingsDto(binaryPath, serverArgs, extraFilePatterns)

    companion object {
        /** The "not detected" hint shows only while the path is blank and the backend reported where it searched. */
        fun binaryComment(page: HephSettingsPageDto): String {
            val searched = page.searched
            return if (page.settings.binaryPath.isBlank() && searched != null) {
                HephBundle.message("settings.binary.notDetected", searched)
            } else {
                HephBundle.message("settings.binary.comment")
            }
        }

        /** Grey text shown inside the empty field: the auto-detected executable. */
        fun binaryPlaceholder(page: HephSettingsPageDto): String =
            if (page.settings.binaryPath.isBlank()) page.detected.orEmpty() else ""
    }
}
