package io.github.hephbuild.heph.rpc

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class HephRpcDtoTest {
    private val settings = HephSettingsDto("/opt/heph", "tool build-lsp", "BUILD2;*.heph")

    private inline fun <reified T> roundTrip(value: T): Pair<T, T> =
        value to Json.decodeFromString<T>(Json.encodeToString(value))

    @Test
    fun jsonRoundTrip() {
        val cases: Map<String, () -> Pair<Any?, Any?>> = mapOf(
            "settings" to { roundTrip(settings) },
            "page with searched" to { roundTrip(HephSettingsPageDto(settings, searched = "PATH, ~/bin")) },
            "page with detected" to { roundTrip(HephSettingsPageDto(settings, detected = "/usr/local/bin/heph")) },
            "page without either" to { roundTrip(HephSettingsPageDto(settings)) },
            "notification" to { roundTrip(HephNotificationDto("title", "content", true)) },
        )
        for ((name, case) in cases) {
            val (original, decoded) = case()
            assertEquals(name, original, decoded)
        }
    }

    @Test
    fun olderPayloadsDecodeWithDefaults() {
        val cases: Map<String, Pair<Any, Any>> = mapOf(
            "settings without fields" to (Json.decodeFromString<HephSettingsDto>("{}") to HephSettingsDto()),
            "page without detection info" to (
                Json.decodeFromString<HephSettingsPageDto>("""{"settings":{"binaryPath":"/opt/heph"}}""") to
                    HephSettingsPageDto(HephSettingsDto(binaryPath = "/opt/heph"))
                ),
            "notification without restartable" to (
                Json.decodeFromString<HephNotificationDto>("""{"title":"t","content":"c"}""") to
                    HephNotificationDto("t", "c", false)
                ),
        )
        for ((name, case) in cases) {
            assertEquals(name, case.second, case.first)
        }
    }
}
