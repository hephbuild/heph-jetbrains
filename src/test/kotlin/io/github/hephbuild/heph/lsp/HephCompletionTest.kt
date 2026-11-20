package io.github.hephbuild.heph.lsp

import org.eclipse.lsp4j.CompletionItemKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HephCompletionTest {
    private data class Case(val kind: CompletionItemKind?, val label: String, val next: Char?, val want: Boolean)

    @Test
    fun shouldAppendParens() {
        val cases = mapOf(
            "function" to Case(CompletionItemKind.Function, "target", null, true),
            "method" to Case(CompletionItemKind.Method, "append", ' ', true),
            "function before a closer" to Case(CompletionItemKind.Function, "len", ')', true),
            "function already called" to Case(CompletionItemKind.Function, "target", '(', false),
            "constant" to Case(CompletionItemKind.Constant, "dict", null, false),
            "load keyword" to Case(CompletionItemKind.Keyword, "load", null, true),
            "load already called" to Case(CompletionItemKind.Keyword, "load", '(', false),
            "other keyword" to Case(CompletionItemKind.Keyword, "if", null, false),
            "variable" to Case(CompletionItemKind.Variable, "x", null, false),
            "no kind" to Case(null, "target", null, false),
        )
        for ((name, case) in cases) {
            assertEquals(name, case.want, HephCompletion.shouldAppendParens(case.kind, case.label, case.next))
        }
    }

    @Test
    fun nextSignificantChar() {
        val cases = mapOf(
            "adjacent" to (("foo(" to 3) to '('),
            "after spaces" to (("foo  (" to 3) to '('),
            "after tab" to (("foo\t(" to 3) to '('),
            "other char" to (("foo)" to 3) to ')'),
            "newline is significant" to (("foo\n(" to 3) to '\n'),
            "end of text" to (("foo" to 3) to null),
            "only blanks" to (("foo  " to 3) to null),
        )
        for ((name, case) in cases) {
            assertEquals(name, case.second, HephCompletion.nextSignificantChar(case.first.first, case.first.second))
        }
    }

    @Test
    fun parensEdit() {
        val cases = mapOf(
            "at end" to (("target" to 6) to ("target()" to 7)),
            "before text" to (("x = target + 1" to 10) to ("x = target() + 1" to 11)),
            "inside a call already" to (("target(name)" to 6) to null),
            "paren after spaces" to (("target  (" to 6) to null),
            "inside brackets" to (("[len]" to 4) to ("[len()]" to 5)),
        )
        for ((name, case) in cases) {
            val actual = HephCompletion.parensEdit(case.first.first, case.first.second)
            assertEquals("$name: ${case.first}", case.second, actual)
        }
        assertNull(HephCompletion.parensEdit("f(", 1))
    }

    @Test
    fun fieldType() {
        val cases = mapOf(
            "string required" to ("target: string (required)" to "string"),
            "string" to ("target: string" to "string"),
            "union keeps everything" to ("target: string | list[string]" to "string | list[string]"),
            "struct with colons inside" to (
                "target: bool | struct(required: bool, notice: string | list[string])" to
                    "bool | struct(required: bool, notice: string | list[string])"
                ),
            "map" to ("target: map[string]" to "map[string]"),
            "bool required" to ("target: bool (required)" to "bool"),
            "extra blanks" to ("target:   string   (required)  " to "string"),
            "no separator" to ("string" to null),
            "empty type" to ("target: " to null),
            "null detail" to (null to null),
        )
        for ((name, case) in cases) {
            assertEquals(name, case.second, HephCompletion.fieldType(case.first))
        }
    }

    private data class FieldCase(
        val kind: CompletionItemKind?,
        val detail: String?,
        val next: Char?,
        val want: HephCompletion.FieldEdit?,
    )

    private val quotesAndComma = HephCompletion.FieldEdit("\"\",", 1, true)
    private val quotesOnly = HephCompletion.FieldEdit("\"\"", 1, true)
    private val commaOnly = HephCompletion.FieldEdit(",", 0, false)

    @Test
    fun fieldEdit() {
        val field = CompletionItemKind.Field
        val cases = mapOf(
            "string required" to FieldCase(field, "target: string (required)", null, quotesAndComma),
            "string optional" to FieldCase(field, "target: string", null, quotesAndComma),
            "string before a closer" to FieldCase(field, "target: string", ')', quotesAndComma),
            "string with blanks before a closer" to FieldCase(field, "target: string", ')', quotesAndComma),
            "string before a comma" to FieldCase(field, "target: string", ',', quotesOnly),
            "string with a double quote after" to FieldCase(field, "target: string", '"', null),
            "string with a single quote after" to FieldCase(field, "target: string", '\'', null),
            "string with equals after" to FieldCase(field, "target: string", '=', null),
            "union" to FieldCase(field, "target: string | list[string]", null, commaOnly),
            "bool or struct" to FieldCase(
                field, "target: bool | struct(required: bool, notice: string | list[string])", null, commaOnly,
            ),
            "bool" to FieldCase(field, "target: bool", null, commaOnly),
            "list" to FieldCase(field, "target: list[string]", ')', commaOnly),
            "map" to FieldCase(field, "target: map[string]", null, commaOnly),
            "struct" to FieldCase(field, "target: struct(a: string)", null, commaOnly),
            "empty detail" to FieldCase(field, "", null, commaOnly),
            "missing detail" to FieldCase(field, null, null, commaOnly),
            "non-string before a comma" to FieldCase(field, "target: bool", ',', null),
            "non-string with equals after" to FieldCase(field, "target: bool", '=', null),
            "non-string with a quote after" to FieldCase(field, "target: list[string]", '"', null),
            "no kind" to FieldCase(null, "target: string", null, null),
            "function kind" to FieldCase(CompletionItemKind.Function, "target: string", null, null),
            "method kind" to FieldCase(CompletionItemKind.Method, null, null, null),
            "constant kind" to FieldCase(CompletionItemKind.Constant, "target: string", null, null),
        )
        for ((name, case) in cases) {
            assertEquals(name, case.want, HephCompletion.fieldEdit(case.kind, case.detail, case.next))
        }
    }

    @Test
    fun applyFieldEdit() {
        val field = CompletionItemKind.Field
        val string = "target: string"
        val list = "target: list[string]"
        val cases = mapOf(
            "string at the end" to (Triple("target(name = ", 14, string) to ("target(name = \"\"," to 15)),
            "string before a closer" to (Triple("f(name = )", 9, string) to ("f(name = \"\",)" to 10)),
            "string before blanks and a closer" to (Triple("f(name =  )", 9, string) to ("f(name = \"\", )" to 10)),
            "string before a comma" to (Triple("f(name = , x)", 9, string) to ("f(name = \"\", x)" to 10)),
            "string with a quote after" to (Triple("f(name = \"x\")", 9, string) to null),
            "string with equals after" to (Triple("f(name = = 1)", 9, string) to null),
            "list at the end" to (Triple("f(labels = ", 11, list) to ("f(labels = ," to 11)),
            "list before a closer" to (Triple("f(labels = )", 11, list) to ("f(labels = ,)" to 11)),
            "list before a comma" to (Triple("f(labels = , x)", 11, list) to null),
        )
        for ((name, case) in cases) {
            val (text, tail, detail) = case.first
            assertEquals("$name: $text", case.second, HephCompletion.applyFieldEdit(text, tail, field, detail))
        }
    }
}
