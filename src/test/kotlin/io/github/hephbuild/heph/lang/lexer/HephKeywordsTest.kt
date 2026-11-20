package io.github.hephbuild.heph.lang.lexer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HephKeywordsTest {
    @Test
    fun keywordsAndBuiltinsAreDisjoint() {
        assertEquals(emptySet<String>(), HephKeywords.KEYWORDS intersect HephKeywords.BUILTINS)
    }

    @Test
    fun tables() {
        val cases = mapOf(
            "heph build file builtins" to (
                HephKeywords.BUILTINS to listOf("target", "file", "glob", "query", "struct", "get_pkg", "provider_state", "heph")
                ),
            "starlark builtins" to (HephKeywords.BUILTINS to listOf("len", "range", "print", "dict", "fail")),
            "starlark keywords" to (HephKeywords.KEYWORDS to listOf("def", "load", "lambda", "for", "in", "not")),
            "python reserved" to (HephKeywords.KEYWORDS to listOf("class", "import", "try", "while", "yield")),
            "constants" to (HephKeywords.KEYWORDS to listOf("True", "False", "None")),
        )
        for ((name, case) in cases) {
            for (word in case.second) assertTrue("$name: $word", word in case.first)
        }
    }
}
