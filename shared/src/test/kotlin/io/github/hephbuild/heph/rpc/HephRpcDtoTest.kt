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
            "page with searched" to { roundTrip(HephSettingsPageDto(settings, "PATH, ~/bin")) },
            "page without searched" to { roundTrip(HephSettingsPageDto(settings, null)) },
            "notification" to { roundTrip(HephNotificationDto("title", "content", true)) },
        )
        for ((name, case) in cases) {
            val (original, decoded) = case()
            assertEquals(name, original, decoded)
        }
    }
}
