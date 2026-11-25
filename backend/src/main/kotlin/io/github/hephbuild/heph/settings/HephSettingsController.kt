package io.github.hephbuild.heph.settings

import io.github.hephbuild.heph.core.HephBinaryMessages
import io.github.hephbuild.heph.core.HephBinaryResolver
import io.github.hephbuild.heph.core.HephBinaryResult
import io.github.hephbuild.heph.rpc.HephSettingsDto
import io.github.hephbuild.heph.rpc.HephSettingsPageDto

/** Settings page logic, free of platform services so it can be tested directly. */
class HephSettingsController(
    private val settings: HephSettings,
    private val resolver: HephBinaryResolver,
    private val afterSave: (patternsChanged: Boolean) -> Unit,
) {
    /** Read-only: a blank binary path stays blank, which keeps meaning auto-detect. */
    fun load(): HephSettingsPageDto = page()

    /** Writes the fields as typed; the resolver trims and unquotes the path when it is used. */
    fun save(dto: HephSettingsDto): HephSettingsPageDto {
        val oldPatterns = settings.extraFilePatterns
        settings.binaryPath = dto.binaryPath
        settings.serverArgs = dto.serverArgs
        settings.extraFilePatterns = dto.extraFilePatterns
        afterSave(oldPatterns != dto.extraFilePatterns)
        return page()
    }

    /** The stored values plus, while the path is blank, the executable auto-detection resolves to. */
    private fun page(): HephSettingsPageDto {
        val stored = HephSettingsDto(settings.binaryPath, settings.serverArgs, settings.extraFilePatterns)
        if (stored.binaryPath.isNotBlank()) return HephSettingsPageDto(stored)
        return when (val detection = resolver.resolve("")) {
            is HephBinaryResult.Found -> HephSettingsPageDto(stored, detected = detection.path.toString())
            is HephBinaryResult.NotFound.AutoDetect ->
                HephSettingsPageDto(stored, searched = HephBinaryMessages.searched(detection))
            else -> HephSettingsPageDto(stored)
        }
    }
}
