package io.github.hephbuild.heph.lang.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HephIndentTest {
    private companion object {
        const val CARET = "<caret>"
        const val UNIT = "    "
    }

    private fun withCaret(text: String, caret: Int) = StringBuilder(text).insert(caret, CARET).toString()

    @Test
    fun enter() {
        val cases = mapOf(
            "empty pair opens onto three lines" to ("target(<caret>)" to "target(\n    <caret>\n)"),
            "nested pair" to ("foo(bar(<caret>))" to "foo(bar(\n    <caret>\n))"),
            "after an item inside brackets" to (
                "target(\n    name = \"a\",<caret>\n)" to "target(\n    name = \"a\",\n    <caret>\n)"
                ),
            "relative to the opener line" to (
                "x = foo(\n    bar(<caret>))" to "x = foo(\n    bar(\n        <caret>\n    ))"
                ),
            "text after the opener moves down" to ("x = [<caret>1, 2]" to "x = [\n    <caret>1, 2]"),
            "blanks after the caret are dropped" to ("foo(<caret>  )" to "foo(\n    <caret>\n)"),
            "mismatched closer is not a pair" to ("foo(<caret>]" to "foo(\n    <caret>]"),
            "dict colon inside braces" to ("x = {\n    \"a\":<caret>" to "x = {\n    \"a\":\n    <caret>"),
            "trailing colon" to ("if x:<caret>" to "if x:\n    <caret>"),
            "trailing colon keeps the line indent" to ("    if x:<caret>" to "    if x:\n        <caret>"),
            "colon before a comment" to ("if x:  # c<caret>" to "if x:  # c\n    <caret>"),
            "plain line keeps default" to ("x = 1<caret>" to null),
            "closed bracket keeps default" to ("foo(a)<caret>" to null),
            "inside a string" to ("x = \"foo(<caret>\"" to null),
            "inside a comment" to ("# foo(<caret> bar" to null),
            "inside a triple-quoted string" to ("x = \"\"\"a(<caret>\nb\"\"\"" to null),
            "bracket in a string is ignored" to ("x = \"(\" + <caret>" to null),
            "bracket in a comment is ignored" to ("# (\nfoo<caret>" to null),
            "bracket in a string after the caret line" to ("x = \"(\"\nfoo<caret>" to null),
            "unbalanced closer" to ("x = )<caret>" to null),
        )
        for ((name, case) in cases) {
            val offset = case.first.indexOf(CARET)
            val text = case.first.replace(CARET, "")
            val actual = HephIndent.applyEnter(text, offset, UNIT)?.let { withCaret(it.first, it.second) }
            assertEquals("$name: Enter in ${case.first}", case.second, actual)
        }
    }

    @Test
    fun enterUsesTheIndentUnit() {
        val text = "foo()"
        val (result, caret) = HephIndent.applyEnter(text, 4, "\t")!!
        assertEquals("tab unit", "foo(\n\t<caret>\n)", withCaret(result, caret))
        assertNull(HephIndent.applyEnter("x = 1", 5, "\t"))
    }

    @Test
    fun closerIndent() {
        val cases = mapOf(
            "closer at the opener indent" to ("foo(\n    )<caret>" to ""),
            "nested closer" to ("foo(\n    bar(\n        )<caret>" to "    "),
            "bracket and brace" to ("x = [\n    {\n        }<caret>" to "    "),
            "line with content" to ("foo(\n    x)<caret>" to null),
            "no opener" to (")<caret>" to null),
            "mismatched opener" to ("foo(\n    ]<caret>" to null),
            "mismatch with nested bracket" to ("{\n    [\n    }<caret>" to null),
            "opener in a string" to ("x = \"(\"\n    )<caret>" to null),
            "opener in a comment" to ("# (\n    )<caret>" to null),
            "tabs" to ("foo(\n\t\t)<caret>" to ""),
        )
        for ((name, case) in cases) {
            val closerOffset = case.first.indexOf(CARET) - 1
            val text = case.first.replace(CARET, "")
            assertEquals("$name: ${case.first}", case.second, HephIndent.closerIndent(text, closerOffset))
        }
    }
}
