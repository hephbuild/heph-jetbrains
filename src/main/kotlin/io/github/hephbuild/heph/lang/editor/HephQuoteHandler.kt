package io.github.hephbuild.heph.lang.editor

import com.intellij.codeInsight.editorActions.MultiCharQuoteHandler
import com.intellij.codeInsight.editorActions.SimpleTokenSetQuoteHandler
import com.intellij.openapi.editor.highlighter.HighlighterIterator
import io.github.hephbuild.heph.lang.lexer.HephTokenTypes

class HephQuoteHandler : SimpleTokenSetQuoteHandler(HephTokenTypes.STRINGS), MultiCharQuoteHandler {
    override fun getClosingQuote(iterator: HighlighterIterator, offset: Int): CharSequence? =
        closingQuote(iterator.document.charsSequence, offset)

    /** The platform default ignores string prefixes: for `r"` it sees a trailing quote and reports the string closed. */
    override fun isNonClosedLiteral(iterator: HighlighterIterator, chars: CharSequence): Boolean =
        isNonClosedToken(chars.subSequence(iterator.start, iterator.end))

    /** The opening quote follows an optional r/b prefix, so it is not at the token start for `r"..."`. */
    override fun isOpeningQuote(iterator: HighlighterIterator, offset: Int): Boolean {
        if (iterator.tokenType !in HephTokenTypes.STRINGS) return false
        val token = iterator.document.charsSequence.subSequence(iterator.start, iterator.end)
        return offset == iterator.start + prefixLength(token)
    }

    /** Also true inside the closing triple quote of a closed triple-quoted string, so typing a quote steps over. */
    override fun isClosingQuote(iterator: HighlighterIterator, offset: Int): Boolean {
        if (super.isClosingQuote(iterator, offset)) return true
        if (iterator.tokenType !in HephTokenTypes.STRINGS) return false
        val token = iterator.document.charsSequence.subSequence(iterator.start, iterator.end)
        return isInClosingTriple(token, offset - iterator.start)
    }

    companion object {
        /** Length of a leading r/b string prefix (at most two letters), 0 if none. */
        fun prefixLength(token: CharSequence): Int {
            var i = 0
            while (i < token.length && i < 2 && token[i].lowercaseChar().let { it == 'r' || it == 'b' }) i++
            return if (i < token.length && (token[i] == '"' || token[i] == '\'')) i else 0
        }

        /**
         * Whether a string token (r/b prefix included) lacks its closing quote. A triple-quoted token counts as closed
         * only when it is at least `""""""` long and ends with the opening quotes.
         */
        fun isNonClosedToken(token: CharSequence): Boolean {
            isNonClosedTripleQuoted(token)?.let { return it }
            val body = token.subSequence(prefixLength(token), token.length)
            return body.length < 2 || body.last() != body[0]
        }

        /** True when [offsetInToken] points at one of the three closing quotes of a closed triple-quoted [token]. */
        fun isInClosingTriple(token: CharSequence, offsetInToken: Int): Boolean =
            isNonClosedTripleQuoted(token) == false && offsetInToken in (token.length - 3) until token.length

        /** Null when [token] (an optional r/b prefix included) does not open with a triple quote. */
        fun isNonClosedTripleQuoted(token: CharSequence): Boolean? {
            val i = prefixLength(token)
            val body = token.subSequence(i, token.length)
            if (body.length < 3) return null
            val quote = body[0]
            if ((quote != '"' && quote != '\'') || body[1] != quote || body[2] != quote) return null
            return !(body.length >= 6 && body.endsWith(quote.toString().repeat(3)))
        }

        /** Closing `"""` or `'''` when the caret at [offset] directly follows three typed quotes (and not a fourth). */
        fun closingQuote(text: CharSequence, offset: Int): CharSequence? {
            if (offset < 3 || offset > text.length) return null
            val quote = text[offset - 1]
            if (quote != '"' && quote != '\'') return null
            if (text[offset - 2] != quote || text[offset - 3] != quote) return null
            if (offset >= 4 && text[offset - 4] == quote) return null
            return quote.toString().repeat(3)
        }
    }
}
