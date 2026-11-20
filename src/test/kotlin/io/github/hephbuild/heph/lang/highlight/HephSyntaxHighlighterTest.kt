package io.github.hephbuild.heph.lang.highlight

import com.intellij.openapi.editor.HighlighterColors
import com.intellij.psi.TokenType
import io.github.hephbuild.heph.lang.lexer.HephLexer
import io.github.hephbuild.heph.lang.lexer.HephTokenTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HephSyntaxHighlighterTest {
    private val highlighter = HephSyntaxHighlighter()

    @Test
    fun tokenKeys() {
        val cases = mapOf(
            "comment" to (HephTokenTypes.COMMENT to HephSyntaxHighlighter.COMMENT),
            "string" to (HephTokenTypes.STRING to HephSyntaxHighlighter.STRING),
            "number" to (HephTokenTypes.NUMBER to HephSyntaxHighlighter.NUMBER),
            "keyword" to (HephTokenTypes.KEYWORD to HephSyntaxHighlighter.KEYWORD),
            "builtin" to (HephTokenTypes.BUILTIN to HephSyntaxHighlighter.BUILTIN),
            "identifier" to (HephTokenTypes.IDENTIFIER to HephSyntaxHighlighter.IDENTIFIER),
            "lpar" to (HephTokenTypes.LPAR to HephSyntaxHighlighter.PARENTHESES),
            "rpar" to (HephTokenTypes.RPAR to HephSyntaxHighlighter.PARENTHESES),
            "lbracket" to (HephTokenTypes.LBRACKET to HephSyntaxHighlighter.BRACKETS),
            "rbrace" to (HephTokenTypes.RBRACE to HephSyntaxHighlighter.BRACES),
            "dot" to (HephTokenTypes.DOT to HephSyntaxHighlighter.DOT),
            "comma" to (HephTokenTypes.COMMA to HephSyntaxHighlighter.COMMA),
            "semicolon" to (HephTokenTypes.SEMICOLON to HephSyntaxHighlighter.SEMICOLON),
            "colon" to (HephTokenTypes.COLON to HephSyntaxHighlighter.OPERATOR),
            "eq" to (HephTokenTypes.EQ to HephSyntaxHighlighter.OPERATOR),
            "operator" to (HephTokenTypes.OPERATOR to HephSyntaxHighlighter.OPERATOR),
        )
        for ((name, case) in cases) {
            assertEquals(name, listOf(case.second), highlighter.getTokenHighlights(case.first).toList())
        }
    }

    @Test
    fun badCharacterWhitespaceAndNull() {
        assertEquals(listOf(HighlighterColors.BAD_CHARACTER), highlighter.getTokenHighlights(TokenType.BAD_CHARACTER).toList())
        assertTrue(highlighter.getTokenHighlights(TokenType.WHITE_SPACE).isEmpty())
        assertTrue(highlighter.getTokenHighlights(null).isEmpty())
    }

    @Test
    fun usesHephLexer() {
        assertTrue(highlighter.highlightingLexer is HephLexer)
    }
}
