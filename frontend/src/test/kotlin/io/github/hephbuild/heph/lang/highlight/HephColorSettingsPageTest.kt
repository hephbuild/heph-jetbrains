package io.github.hephbuild.heph.lang.highlight

import com.intellij.psi.TokenType
import io.github.hephbuild.heph.lang.HephFileType
import io.github.hephbuild.heph.lang.HephIcons
import io.github.hephbuild.heph.lang.lexer.HephLexer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HephColorSettingsPageTest {
    private val page = HephColorSettingsPage()

    @Test
    fun descriptorsHaveNamesAndDistinctKeys() {
        val descriptors = page.attributeDescriptors
        assertTrue(descriptors.isNotEmpty())
        assertTrue(descriptors.all { it.displayName.isNotBlank() })
        assertEquals(descriptors.size, descriptors.map { it.key }.toSet().size)
    }

    @Test
    fun everyHighlighterKeyIsConfigurable() {
        val configurable = page.attributeDescriptors.map { it.key }.toSet()
        val used = listOf(
            HephSyntaxHighlighter.COMMENT, HephSyntaxHighlighter.STRING, HephSyntaxHighlighter.NUMBER,
            HephSyntaxHighlighter.KEYWORD, HephSyntaxHighlighter.BUILTIN, HephSyntaxHighlighter.IDENTIFIER,
            HephSyntaxHighlighter.PARENTHESES, HephSyntaxHighlighter.BRACKETS, HephSyntaxHighlighter.BRACES,
            HephSyntaxHighlighter.DOT, HephSyntaxHighlighter.COMMA, HephSyntaxHighlighter.SEMICOLON,
            HephSyntaxHighlighter.OPERATOR,
        )
        for (key in used) assertTrue(key.externalName, key in configurable)
    }

    @Test
    fun demoTextLexesWithoutBadCharacters() {
        val lexer = HephLexer()
        lexer.start(page.demoText)
        while (lexer.tokenType != null) {
            assertFalse(lexer.tokenType == TokenType.BAD_CHARACTER)
            lexer.advance()
        }
    }

    @Test
    fun pageMetadata() {
        assertTrue(page.highlighter is HephSyntaxHighlighter)
        assertEquals(HephIcons.FILE, page.icon)
        assertEquals(HephIcons.FILE, HephFileType.icon)
        assertEquals("Heph", page.displayName)
    }
}
