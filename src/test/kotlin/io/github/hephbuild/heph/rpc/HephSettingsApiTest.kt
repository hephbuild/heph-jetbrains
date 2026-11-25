package io.github.hephbuild.heph.rpc

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.github.hephbuild.heph.settings.HephSettings
import kotlinx.coroutines.runBlocking

/** Resolves the API through the registry (in-process in a normal IDE); `save` is covered by `HephSettingsControllerTest` since it schedules IDE-wide side effects. */
class HephSettingsApiTest : BasePlatformTestCase() {
    fun testLoadReturnsStoredValues() {
        val settings = HephSettings.getInstance()
        val before = HephSettings.State().also { it.copyFrom(settings.state) }
        try {
            settings.serverArgs = "tool build-lsp --debug"
            settings.extraFilePatterns = "BUILD2"

            val page = runBlocking { HephSettingsApi.getInstance().load() }

            assertEquals(settings.binaryPath, page.settings.binaryPath)
            assertEquals("tool build-lsp --debug", page.settings.serverArgs)
            assertEquals("BUILD2", page.settings.extraFilePatterns)
            assertEquals(page.settings.binaryPath.isBlank(), page.searched != null)
        } finally {
            settings.loadState(before)
        }
    }
}
