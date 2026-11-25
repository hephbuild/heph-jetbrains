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
    /** Silent first-run seeding of the binary path; persists the seed. */
    fun load(): HephSettingsPageDto {
        val detection = resolver.resolve("")
        val detected = (detection as? HephBinaryResult.Found)?.path?.toString()
        val seeded = HephSettings.seedBinaryPath(settings.binaryPath, detected)
        if (seeded != settings.binaryPath) settings.binaryPath = seeded
        val searched = if (seeded.isBlank()) {
            (detection as? HephBinaryResult.NotFound.AutoDetect)?.let(HephBinaryMessages::searched)
        } else {
            null
        }
        return HephSettingsPageDto(settings.toDto(), searched)
    }

    /** Writes the fields as typed; the resolver trims and unquotes the path when it is used. */
    fun save(dto: HephSettingsDto): HephSettingsPageDto {
        val oldPatterns = settings.extraFilePatterns
        settings.binaryPath = dto.binaryPath
        settings.serverArgs = dto.serverArgs
        settings.extraFilePatterns = dto.extraFilePatterns
        afterSave(oldPatterns != dto.extraFilePatterns)
        return HephSettingsPageDto(settings.toDto(), null)
    }

    private fun HephSettings.toDto() = HephSettingsDto(binaryPath, serverArgs, extraFilePatterns)
}
