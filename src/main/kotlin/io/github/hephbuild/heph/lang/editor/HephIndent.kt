package io.github.hephbuild.heph.lang.editor

import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import io.github.hephbuild.heph.lang.lexer.HephLexer
import io.github.hephbuild.heph.lang.lexer.HephTokenTypes

/** Lexer-driven indentation rules, free of platform editor APIs. Brackets in strings and comments are ignored. */
object HephIndent {
    /** [text] replaces the whitespace right after the caret; the caret lands [caretInText] characters into it. */
    class EnterEdit(val text: String, val caretInText: Int)

    private class Opener(val offset: Int, val char: Char)

    private class Scan(val openers: List<Opener>, val lastSignificant: IElementType?, val insideLiteral: Boolean)

    private val CLOSER_OF = mapOf('(' to ')', '[' to ']', '{' to '}')

    /** The edit for Enter at [offset], or null to keep the platform's default behaviour. */
    fun enter(text: CharSequence, offset: Int, unit: String): EnterEdit? {
        val scan = scan(text, offset)
        if (scan.insideLiteral) return null

        val opener = scan.openers.lastOrNull()
        if (opener != null) {
            val openerIndent = indentOfLineAt(text, opener.offset)
            val inner = openerIndent + unit
            val next = nextNonBlank(text, offset)
            return if (next < text.length && text[next] == CLOSER_OF[opener.char]) {
                EnterEdit("\n$inner\n$openerIndent", 1 + inner.length)
            } else {
                EnterEdit("\n$inner", 1 + inner.length)
            }
        }
        if (scan.lastSignificant == HephTokenTypes.COLON) {
            val inner = indentOfLineAt(text, offset) + unit
            return EnterEdit("\n$inner", 1 + inner.length)
        }
        return null
    }

    /** Applies [enter] to a string; returns the new text and caret offset. For tests and callers without a document. */
    fun applyEnter(text: String, offset: Int, unit: String): Pair<String, Int>? {
        val edit = enter(text, offset, unit) ?: return null
        var end = offset
        while (end < text.length && (text[end] == ' ' || text[end] == '\t')) end++
        return (text.substring(0, offset) + edit.text + text.substring(end)) to (offset + edit.caretInText)
    }

    /**
     * Indent for the closing bracket at [closerOffset] when it is the first thing on its line: the indent of the
     * line holding its opener. Null when the line has other content or no matching opener exists.
     */
    fun closerIndent(text: CharSequence, closerOffset: Int): String? {
        val closer = text[closerOffset]
        if (closer != ')' && closer != ']' && closer != '}') return null
        val lineStart = lineStart(text, closerOffset)
        if ((lineStart until closerOffset).any { text[it] != ' ' && text[it] != '\t' }) return null
        val scan = scan(text, closerOffset)
        if (scan.insideLiteral) return null
        val opener = scan.openers.lastOrNull() ?: return null
        if (CLOSER_OF[opener.char] != closer) return null
        return indentOfLineAt(text, opener.offset)
    }

    fun lineStart(text: CharSequence, offset: Int): Int {
        var i = offset
        while (i > 0 && text[i - 1] != '\n' && text[i - 1] != '\r') i--
        return i
    }

    private fun indentOfLineAt(text: CharSequence, offset: Int): String {
        val start = lineStart(text, offset)
        var end = start
        while (end < text.length && (text[end] == ' ' || text[end] == '\t')) end++
        return text.subSequence(start, end).toString()
    }

    private fun nextNonBlank(text: CharSequence, offset: Int): Int {
        var i = offset
        while (i < text.length && (text[i] == ' ' || text[i] == '\t')) i++
        return i
    }

    private fun scan(text: CharSequence, offset: Int): Scan {
        val openers = ArrayList<Opener>()
        var lastSignificant: IElementType? = null
        var lastStart = 0
        var lastType: IElementType? = null
        val lexer = HephLexer()
        lexer.start(text, 0, offset, 0)
        while (true) {
            val type = lexer.tokenType ?: break
            val start = lexer.tokenStart
            when (type) {
                HephTokenTypes.LPAR, HephTokenTypes.LBRACKET, HephTokenTypes.LBRACE -> openers += Opener(start, text[start])
                HephTokenTypes.RPAR, HephTokenTypes.RBRACKET, HephTokenTypes.RBRACE -> {
                    val top = openers.lastOrNull()
                    if (top != null && CLOSER_OF[top.char] == text[start]) openers.removeAt(openers.lastIndex)
                }
            }
            if (type != TokenType.WHITE_SPACE && type != HephTokenTypes.COMMENT) lastSignificant = type
            lastStart = start
            lastType = type
            lexer.advance()
        }
        val inside = when (lastType) {
            HephTokenTypes.STRING ->
                HephQuoteHandler.isNonClosedToken(text.subSequence(lastStart, offset))
            HephTokenTypes.COMMENT -> offset < text.length && text[offset] != '\n' && text[offset] != '\r'
            else -> false
        }
        return Scan(openers, lastSignificant, inside)
    }
}
