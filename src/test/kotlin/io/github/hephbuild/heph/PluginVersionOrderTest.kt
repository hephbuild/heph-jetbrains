package io.github.hephbuild.heph

import com.intellij.openapi.util.text.StringUtil
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards the nightly version scheme from .github/workflows/version.sh against the IDE's version ordering. */
class PluginVersionOrderTest {
    @Test
    fun ordering() {
        val cases = mapOf(
            "nightly is below the release it leads up to" to ("1.4.0-nightly.20261124153012" to "1.4.0"),
            "nightly is above the previous release" to ("1.3.2" to "1.4.0-nightly.20261124153012"),
            "later nightly is above earlier one" to ("1.4.0-nightly.20261124153012" to "1.4.0-nightly.20261124153013"),
            "nightly on a later day is above" to ("1.4.0-nightly.20261124235959" to "1.4.0-nightly.20261125000000"),
            "nightly of the next version is above this release" to ("1.4.0" to "1.5.0-nightly.20261201000000"),
        )
        for ((name, pair) in cases) {
            val (lower, higher) = pair
            assertTrue("$name: $lower < $higher", StringUtil.compareVersionNumbers(lower, higher) < 0)
            assertTrue("$name: $higher > $lower", StringUtil.compareVersionNumbers(higher, lower) > 0)
        }
    }
}
