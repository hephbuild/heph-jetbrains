package io.github.hephbuild.heph.lang.lexer

import com.intellij.lexer.Lexer
import com.intellij.psi.TokenType
import com.intellij.testFramework.LexerTestCase

class HephLexerTest : LexerTestCase() {
    override fun createLexer(): Lexer = HephLexer()

    override fun getDirPath(): String = ""

    private fun lex(text: String): List<Pair<String, String>> {
        val lexer = HephLexer()
        lexer.start(text)
        val tokens = mutableListOf<Pair<String, String>>()
        while (lexer.tokenType != null) {
            tokens += lexer.tokenType!!.toString() to text.substring(lexer.tokenStart, lexer.tokenEnd)
            lexer.advance()
        }
        return tokens
    }

    private fun ws(text: String) = TokenType.WHITE_SPACE.toString() to text

    fun testTokens() {
        val cases = mapOf(
            "empty" to ("" to emptyList()),
            "comment" to ("# hi there" to listOf("COMMENT" to "# hi there")),
            "comment then code" to (
                "# c\nx" to listOf("COMMENT" to "# c", ws("\n"), "IDENTIFIER" to "x")
                ),
            "keywords builtins identifiers" to (
                "def target foo" to listOf(
                    "KEYWORD" to "def", ws(" "), "BUILTIN" to "target", ws(" "), "IDENTIFIER" to "foo",
                )
                ),
            "constants and python-only reserved words" to (
                "True None yield" to listOf(
                    "KEYWORD" to "True", ws(" "), "KEYWORD" to "None", ws(" "), "KEYWORD" to "yield",
                )
                ),
            "builtin after dot" to (
                "x.target" to listOf("IDENTIFIER" to "x", "DOT" to ".", "IDENTIFIER" to "target")
                ),
            "builtin after spaced dot" to (
                "x . target" to listOf(
                    "IDENTIFIER" to "x", ws(" "), "DOT" to ".", ws(" "), "IDENTIFIER" to "target",
                )
                ),
            "builtin after dot resets" to (
                "x.a target" to listOf(
                    "IDENTIFIER" to "x", "DOT" to ".", "IDENTIFIER" to "a", ws(" "), "BUILTIN" to "target",
                )
                ),
            "heph core call" to (
                "heph.core.pkg()" to listOf(
                    "BUILTIN" to "heph", "DOT" to ".", "IDENTIFIER" to "core", "DOT" to ".",
                    "IDENTIFIER" to "pkg", "LPAR" to "(", "RPAR" to ")",
                )
                ),
            "numbers" to (
                "1 0x1F 0o17 0b101 1.5 .5 1e3 2.5E-2 1_000" to listOf(
                    "NUMBER" to "1", ws(" "), "NUMBER" to "0x1F", ws(" "), "NUMBER" to "0o17", ws(" "),
                    "NUMBER" to "0b101", ws(" "), "NUMBER" to "1.5", ws(" "), "NUMBER" to ".5", ws(" "),
                    "NUMBER" to "1e3", ws(" "), "NUMBER" to "2.5E-2", ws(" "), "NUMBER" to "1_000",
                )
                ),
            "double quoted with escape" to (
                "\"a\\\"b\"" to listOf("STRING" to "\"a\\\"b\"")
                ),
            "single quoted" to ("'x'" to listOf("STRING" to "'x'")),
            "string prefixes" to (
                "r'a' b\"b\" rb'c' BR\"d\"" to listOf(
                    "STRING" to "r'a'", ws(" "), "STRING" to "b\"b\"", ws(" "), "STRING" to "rb'c'", ws(" "),
                    "STRING" to "BR\"d\"",
                )
                ),
            "prefix-like identifier" to (
                "rb_x 'a'" to listOf("IDENTIFIER" to "rb_x", ws(" "), "STRING" to "'a'")
                ),
            "unterminated at end of line" to (
                "\"abc\nx" to listOf("STRING" to "\"abc", ws("\n"), "IDENTIFIER" to "x")
                ),
            "empty triple" to ("\"\"\"\"\"\"" to listOf("STRING" to "\"\"\"\"\"\"")),
            "triple with embedded quotes" to (
                "\"\"\"a \"b\" ''' c\"\"\"" to listOf("STRING" to "\"\"\"a \"b\" ''' c\"\"\"")
                ),
            "triple multi-line" to (
                "'''a\nb'''x" to listOf("STRING" to "'''a\nb'''", "IDENTIFIER" to "x")
                ),
            "triple unterminated runs to EOF" to (
                "\"\"\"abc\ndef" to listOf("STRING" to "\"\"\"abc\ndef")
                ),
            "heph expression in string" to (
                "\"\${read://:v}\"" to listOf("STRING" to "\"\${read://:v}\"")
                ),
            "line continuation" to (
                "a \\\nb" to listOf("IDENTIFIER" to "a", ws(" \\\n"), "IDENTIFIER" to "b")
                ),
            "operators" to (
                "a==b //= c ** d -> e != f" to listOf(
                    "IDENTIFIER" to "a", "OPERATOR" to "==", "IDENTIFIER" to "b", ws(" "), "OPERATOR" to "//=",
                    ws(" "), "IDENTIFIER" to "c", ws(" "), "OPERATOR" to "**", ws(" "), "IDENTIFIER" to "d", ws(" "),
                    "OPERATOR" to "->", ws(" "), "IDENTIFIER" to "e", ws(" "), "OPERATOR" to "!=", ws(" "),
                    "IDENTIFIER" to "f",
                )
                ),
            "assignment and punctuation" to (
                "x = [1, 2]; {a: b}" to listOf(
                    "IDENTIFIER" to "x", ws(" "), "EQ" to "=", ws(" "), "LBRACKET" to "[", "NUMBER" to "1",
                    "COMMA" to ",", ws(" "), "NUMBER" to "2", "RBRACKET" to "]", "SEMICOLON" to ";", ws(" "),
                    "LBRACE" to "{", "IDENTIFIER" to "a", "COLON" to ":", ws(" "), "IDENTIFIER" to "b",
                    "RBRACE" to "}",
                )
                ),
            "bad characters" to (
                "! \\x" to listOf(
                    TokenType.BAD_CHARACTER.toString() to "!", ws(" "), TokenType.BAD_CHARACTER.toString() to "\\",
                    "IDENTIFIER" to "x",
                )
                ),
            "backslash crlf continues single-quoted string" to (
                "'a\\\r\nb' c" to listOf("STRING" to "'a\\\r\nb'", ws(" "), "IDENTIFIER" to "c")
                ),
            "backslash lf continues double-quoted string" to (
                "\"a\\\nb\"" to listOf("STRING" to "\"a\\\nb\"")
                ),
            "surrogate pair is one bad character" to (
                "x\uD83D\uDE00y" to listOf(
                    "IDENTIFIER" to "x", TokenType.BAD_CHARACTER.toString() to "\uD83D\uDE00", "IDENTIFIER" to "y",
                )
                ),
            "lone surrogate is one bad character" to (
                "\uD83D" to listOf(TokenType.BAD_CHARACTER.toString() to "\uD83D")
                ),
            "crlf" to (
                "a\r\nb" to listOf("IDENTIFIER" to "a", ws("\r\n"), "IDENTIFIER" to "b")
                ),
        )
        for ((name, case) in cases) {
            val (input, expected) = case
            assertEquals(name, expected, lex(input))
            checkCorrectRestart(input)
            checkZeroState(input, HephTokenTypes.STRINGS)
            checkZeroState(input, HephTokenTypes.COMMENTS)
        }
    }

    fun testTokensCoverInputWithoutGaps() {
        val inputs = listOf("target(name = 'x', deps = [\"//a:b\"]) # c\n\"\"\"unterminated", "\\", "'", "0x", "1e", "..")
        for (input in inputs) {
            assertEquals(input, input, lex(input).joinToString("") { it.second })
        }
    }
}
