package io.github.hephbuild.heph.lang.parser

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import io.github.hephbuild.heph.lang.HephLanguage
import io.github.hephbuild.heph.lang.lexer.HephLexer
import io.github.hephbuild.heph.lang.lexer.HephTokenTypes
import io.github.hephbuild.heph.lang.psi.HephFile

/** Flat PSI: the language server does the real analysis, the file is one node holding all tokens. */
class HephParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = HephLexer()

    override fun createParser(project: Project?): PsiParser = PsiParser { root, builder -> parseFlat(root, builder) }

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getCommentTokens(): TokenSet = HephTokenTypes.COMMENTS

    override fun getStringLiteralElements(): TokenSet = HephTokenTypes.STRINGS

    override fun createElement(node: ASTNode): PsiElement = ASTWrapperPsiElement(node)

    override fun createFile(viewProvider: FileViewProvider): PsiFile = HephFile(viewProvider)

    companion object {
        val FILE = IFileElementType(HephLanguage)

        private fun parseFlat(root: IElementType, builder: PsiBuilder): ASTNode {
            val marker = builder.mark()
            while (!builder.eof()) builder.advanceLexer()
            marker.done(root)
            return builder.treeBuilt
        }
    }
}
