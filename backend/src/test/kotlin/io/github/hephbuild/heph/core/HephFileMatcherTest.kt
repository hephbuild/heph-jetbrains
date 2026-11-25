package io.github.hephbuild.heph.core

import org.junit.Assert.assertEquals
import org.junit.Test

class HephFileMatcherTest {
    private data class Case(val extra: String, val name: String, val want: Boolean)

    @Test
    fun matches() {
        val cases = mapOf(
            "plain BUILD" to Case("", "BUILD", true),
            "BUILD dot suffix" to Case("", "BUILD.foo", true),
            "dot BUILD extension" to Case("", "x.BUILD", true),
            "BUILD2 without extra" to Case("", "BUILD2", false),
            "BUILD2 with extra" to Case("BUILD2", "BUILD2", true),
            "lowercase build" to Case("", "build", false),
            "BUILDING" to Case("", "BUILDING", false),
            "python file" to Case("", "foo.py", false),
            "extra glob" to Case("*.heph", "pkg.heph", true),
            "extra glob miss" to Case("*.heph", "pkg.hephx", false),
            "semicolon split first" to Case("BUILD2; OTHER", "BUILD2", true),
            "semicolon split second" to Case("BUILD2; OTHER", "OTHER", true),
            "regex chars are literal" to Case("a+b", "aab", false),
            "question mark" to Case("BUILD?", "BUILD3", true),
        )
        for ((name, case) in cases) {
            assertEquals(name, case.want, HephFileMatcher(case.extra).matches(case.name))
        }
    }
}
