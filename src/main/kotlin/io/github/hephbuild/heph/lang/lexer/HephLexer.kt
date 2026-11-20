package io.github.hephbuild.heph.lang.lexer

import com.intellij.lexer.LexerBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

/**
 * Hand-written lexer for heph BUILD files (Starlark). State is 1 right after a DOT (whitespace keeps it), else 0.
 * Unterminated strings stop at end of line, unterminated triple-quoted strings run to the end of the buffer.
 */
class HephLexer : LexerBase() {
    private var buffer: CharSequence = ""
    private var endOffset = 0
    private var state = 0
    private var tokenStart = 0
    private var tokenEnd = 0
    private var tokenType: IElementType? = null

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.endOffset = endOffset
        this.state = initialState
        this.tokenStart = startOffset
        this.tokenEnd = startOffset
        this.tokenType = null
        locateToken()
    }

    override fun getState(): Int = state

    override fun getTokenType(): IElementType? = tokenType

    override fun getTokenStart(): Int = tokenStart

    override fun getTokenEnd(): Int = tokenEnd

    override fun advance() {
        state = when (tokenType) {
            HephTokenTypes.DOT -> 1
            TokenType.WHITE_SPACE -> state
            else -> 0
        }
        tokenStart = tokenEnd
        locateToken()
    }

    override fun getBufferSequence(): CharSequence = buffer

    override fun getBufferEnd(): Int = endOffset

    private fun at(i: Int): Char = if (i < endOffset) buffer[i] else NUL

    private fun locateToken() {
        if (tokenStart >= endOffset) {
            tokenType = null
            tokenEnd = tokenStart
            return
        }
        val (type, end) = lex(tokenStart)
        tokenType = type
        tokenEnd = end
    }

    private fun lex(start: Int): Pair<IElementType, Int> {
        val c = at(start)
        return when {
            isWhitespaceStart(start) -> TokenType.WHITE_SPACE to skipWhitespace(start)
            c == '#' -> HephTokenTypes.COMMENT to skipLine(start)
            c == '"' || c == '\'' -> HephTokenTypes.STRING to skipString(start, start)
            c.isDigit() || (c == '.' && at(start + 1).isDigit()) -> HephTokenTypes.NUMBER to skipNumber(start)
            isIdentifierStart(c) -> lexIdentifier(start)
            else -> lexPunctuation(start, c)
        }
    }

    private fun isWhitespaceStart(i: Int): Boolean {
        val c = at(i)
        return c == ' ' || c == '\t' || c == '\u000c' || c == '\r' || c == '\n' || lineContinuationLength(i) > 0
    }

    private fun lineContinuationLength(i: Int): Int {
        if (at(i) != '\\') return 0
        return when {
            at(i + 1) == '\r' && at(i + 2) == '\n' -> 3
            at(i + 1) == '\n' || at(i + 1) == '\r' -> 2
            else -> 0
        }
    }

    private fun skipWhitespace(start: Int): Int {
        var i = start
        while (i < endOffset) {
            val c = buffer[i]
            if (c == ' ' || c == '\t' || c == '\u000c' || c == '\r' || c == '\n') {
                i++
                continue
            }
            val continuation = lineContinuationLength(i)
            if (continuation == 0) break
            i += continuation
        }
        return i
    }

    private fun skipLine(start: Int): Int {
        var i = start
        while (i < endOffset && buffer[i] != '\n' && buffer[i] != '\r') i++
        return i
    }

    /** [quoteStart] is the index of the opening quote, [start] the token start (a prefix may precede the quote). */
    private fun skipString(start: Int, quoteStart: Int): Int {
        val quote = buffer[quoteStart]
        val triple = at(quoteStart + 1) == quote && at(quoteStart + 2) == quote
        var i = quoteStart + if (triple) 3 else 1
        while (i < endOffset) {
            val c = buffer[i]
            when {
                c == '\\' -> i += when {
                    at(i + 1) == '\r' && at(i + 2) == '\n' -> 3
                    i + 1 < endOffset -> 2
                    else -> 1
                }
                triple && c == quote && at(i + 1) == quote && at(i + 2) == quote -> return i + 3
                !triple && c == quote -> return i + 1
                !triple && (c == '\n' || c == '\r') -> return i
                else -> i++
            }
        }
        return minOf(i, endOffset).coerceAtLeast(start)
    }

    private fun skipNumber(start: Int): Int {
        var i = start
        if (at(i) == '0' && at(i + 1).lowercaseChar() in "xob") {
            val radixDigits: (Char) -> Boolean = when (at(i + 1).lowercaseChar()) {
                'x' -> { ch -> ch.isDigit() || ch.lowercaseChar() in 'a'..'f' }
                'o' -> { ch -> ch in '0'..'7' }
                else -> { ch -> ch == '0' || ch == '1' }
            }
            i += 2
            while (i < endOffset && (radixDigits(buffer[i]) || buffer[i] == '_')) i++
            return i
        }
        while (at(i).isDigit() || at(i) == '_') i++
        if (at(i) == '.') {
            i++
            while (at(i).isDigit() || at(i) == '_') i++
        }
        if (at(i) == 'e' || at(i) == 'E') {
            var j = i + 1
            if (at(j) == '+' || at(j) == '-') j++
            if (at(j).isDigit()) {
                while (at(j).isDigit()) j++
                i = j
            }
        }
        return i
    }

    private fun isIdentifierStart(c: Char) = c == '_' || c.isLetter()

    private fun isIdentifierPart(c: Char) = c == '_' || c.isLetterOrDigit()

    private fun lexIdentifier(start: Int): Pair<IElementType, Int> {
        var i = start
        while (i < endOffset && isIdentifierPart(buffer[i])) i++
        val text = buffer.subSequence(start, i).toString()
        val next = at(i)
        if ((next == '"' || next == '\'') && text.lowercase() in STRING_PREFIXES) {
            return HephTokenTypes.STRING to skipString(start, i)
        }
        val type = when {
            text in HephKeywords.KEYWORDS -> HephTokenTypes.KEYWORD
            text in HephKeywords.BUILTINS && state != 1 -> HephTokenTypes.BUILTIN
            else -> HephTokenTypes.IDENTIFIER
        }
        return type to i
    }

    private fun lexPunctuation(start: Int, c: Char): Pair<IElementType, Int> {
        val single = when (c) {
            '(' -> HephTokenTypes.LPAR
            ')' -> HephTokenTypes.RPAR
            '[' -> HephTokenTypes.LBRACKET
            ']' -> HephTokenTypes.RBRACKET
            '{' -> HephTokenTypes.LBRACE
            '}' -> HephTokenTypes.RBRACE
            '.' -> HephTokenTypes.DOT
            ',' -> HephTokenTypes.COMMA
            ':' -> HephTokenTypes.COLON
            ';' -> HephTokenTypes.SEMICOLON
            else -> null
        }
        if (single != null) return single to start + 1

        for (length in 3 downTo 2) {
            if (start + length <= endOffset && buffer.subSequence(start, start + length).toString() in OPERATORS) {
                return HephTokenTypes.OPERATOR to start + length
            }
        }
        return when {
            c == '=' -> HephTokenTypes.EQ to start + 1
            c in SINGLE_OPERATORS -> HephTokenTypes.OPERATOR to start + 1
            else -> TokenType.BAD_CHARACTER to start + badCharacterLength(start, c)
        }
    }

    /** A surrogate pair (e.g. an emoji) is one character. */
    private fun badCharacterLength(start: Int, c: Char): Int =
        if (Character.isHighSurrogate(c) && start + 1 < endOffset && Character.isLowSurrogate(buffer[start + 1])) 2 else 1

    private companion object {
        const val NUL = '\u0000'
        val STRING_PREFIXES = setOf("r", "b", "rb", "br")
        val SINGLE_OPERATORS = "+-*/%&|^~<>@"
        val OPERATORS = setOf(
            "**=", "//=", ">>=", "<<=",
            "==", "!=", "<=", ">=", "**", "//", "<<", ">>", "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "->",
        )
    }
}
