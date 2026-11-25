package io.github.hephbuild.heph.rpc

import io.github.hephbuild.heph.core.HephBinaryResolver
import io.github.hephbuild.heph.settings.HephSettings
import io.github.hephbuild.heph.settings.HephSettingsController
import io.github.hephbuild.heph.settings.HephSettingsSideEffects
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HephSettingsApiImpl : HephSettingsApi {
    private val controller by lazy {
        HephSettingsController(HephSettings.getInstance(), HephBinaryResolver(), HephSettingsSideEffects::schedule)
    }

    override suspend fun load(): HephSettingsPageDto = withContext(Dispatchers.IO) { controller.load() }

    override suspend fun save(settings: HephSettingsDto): HephSettingsPageDto =
        withContext(Dispatchers.IO) { controller.save(settings) }
}
