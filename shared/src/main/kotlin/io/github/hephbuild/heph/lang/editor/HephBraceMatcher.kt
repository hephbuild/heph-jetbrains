package io.github.hephbuild.heph.lang.editor

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import io.github.hephbuild.heph.lang.lexer.HephTokenTypes

class HephBraceMatcher : PairedBraceMatcher {
    override fun getPairs(): Array<BracePair> = PAIRS

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true

    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int = openingBraceOffset

    private companion object {
        val PAIRS = arrayOf(
            BracePair(HephTokenTypes.LPAR, HephTokenTypes.RPAR, false),
            BracePair(HephTokenTypes.LBRACKET, HephTokenTypes.RBRACKET, false),
            BracePair(HephTokenTypes.LBRACE, HephTokenTypes.RBRACE, true),
        )
    }
}
