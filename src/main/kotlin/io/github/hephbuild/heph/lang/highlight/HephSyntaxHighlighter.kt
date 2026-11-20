package io.github.hephbuild.heph.lang.highlight

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import io.github.hephbuild.heph.lang.lexer.HephLexer
import io.github.hephbuild.heph.lang.lexer.HephTokenTypes

class HephSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = HephLexer()

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> =
        when (tokenType) {
            null -> EMPTY
            TokenType.BAD_CHARACTER -> BAD
            else -> TOKEN_KEYS[tokenType]?.let { pack(it) } ?: EMPTY
        }

    companion object {
        val COMMENT = createTextAttributesKey("HEPH_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
        val STRING = createTextAttributesKey("HEPH_STRING", DefaultLanguageHighlighterColors.STRING)
        val NUMBER = createTextAttributesKey("HEPH_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
        val KEYWORD = createTextAttributesKey("HEPH_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
        val BUILTIN = createTextAttributesKey("HEPH_BUILTIN", DefaultLanguageHighlighterColors.PREDEFINED_SYMBOL)
        val IDENTIFIER = createTextAttributesKey("HEPH_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER)
        val PARENTHESES = createTextAttributesKey("HEPH_PARENTHESES", DefaultLanguageHighlighterColors.PARENTHESES)
        val BRACKETS = createTextAttributesKey("HEPH_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS)
        val BRACES = createTextAttributesKey("HEPH_BRACES", DefaultLanguageHighlighterColors.BRACES)
        val DOT = createTextAttributesKey("HEPH_DOT", DefaultLanguageHighlighterColors.DOT)
        val COMMA = createTextAttributesKey("HEPH_COMMA", DefaultLanguageHighlighterColors.COMMA)
        val SEMICOLON = createTextAttributesKey("HEPH_SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON)
        val OPERATOR = createTextAttributesKey("HEPH_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN)

        private val EMPTY = emptyArray<TextAttributesKey>()
        private val BAD = arrayOf(HighlighterColors.BAD_CHARACTER)

        private val TOKEN_KEYS: Map<IElementType, TextAttributesKey> = mapOf(
            HephTokenTypes.COMMENT to COMMENT,
            HephTokenTypes.STRING to STRING,
            HephTokenTypes.NUMBER to NUMBER,
            HephTokenTypes.KEYWORD to KEYWORD,
            HephTokenTypes.BUILTIN to BUILTIN,
            HephTokenTypes.IDENTIFIER to IDENTIFIER,
            HephTokenTypes.LPAR to PARENTHESES,
            HephTokenTypes.RPAR to PARENTHESES,
            HephTokenTypes.LBRACKET to BRACKETS,
            HephTokenTypes.RBRACKET to BRACKETS,
            HephTokenTypes.LBRACE to BRACES,
            HephTokenTypes.RBRACE to BRACES,
            HephTokenTypes.DOT to DOT,
            HephTokenTypes.COMMA to COMMA,
            HephTokenTypes.SEMICOLON to SEMICOLON,
            HephTokenTypes.COLON to OPERATOR,
            HephTokenTypes.EQ to OPERATOR,
            HephTokenTypes.OPERATOR to OPERATOR,
        )
    }
}
