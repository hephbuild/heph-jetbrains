package io.github.hephbuild.heph.lang.lexer

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import io.github.hephbuild.heph.lang.HephLanguage

class HephTokenType(debugName: String) : IElementType(debugName, HephLanguage)

object HephTokenTypes {
    val COMMENT = HephTokenType("COMMENT")
    val STRING = HephTokenType("STRING")
    val NUMBER = HephTokenType("NUMBER")
    val IDENTIFIER = HephTokenType("IDENTIFIER")
    val KEYWORD = HephTokenType("KEYWORD")
    val BUILTIN = HephTokenType("BUILTIN")
    val LPAR = HephTokenType("LPAR")
    val RPAR = HephTokenType("RPAR")
    val LBRACKET = HephTokenType("LBRACKET")
    val RBRACKET = HephTokenType("RBRACKET")
    val LBRACE = HephTokenType("LBRACE")
    val RBRACE = HephTokenType("RBRACE")
    val DOT = HephTokenType("DOT")
    val COMMA = HephTokenType("COMMA")
    val COLON = HephTokenType("COLON")
    val SEMICOLON = HephTokenType("SEMICOLON")
    val EQ = HephTokenType("EQ")
    val OPERATOR = HephTokenType("OPERATOR")

    val COMMENTS: TokenSet = TokenSet.create(COMMENT)
    val STRINGS: TokenSet = TokenSet.create(STRING)
}
