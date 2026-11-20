package io.github.hephbuild.heph.lang.editor

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import io.github.hephbuild.heph.lang.psi.HephFile

/** Typing a closing bracket on a whitespace-only line moves it to the indent of the line holding its opener. */
class HephClosingBracketHandler : TypedHandlerDelegate() {
    override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): TypedHandlerDelegate.Result {
        if (file !is HephFile || (c != ')' && c != ']' && c != '}')) return TypedHandlerDelegate.Result.CONTINUE
        val document = editor.document
        val text = document.charsSequence
        val closerOffset = editor.caretModel.offset - 1
        if (closerOffset < 0 || text[closerOffset] != c) return TypedHandlerDelegate.Result.CONTINUE
        val indent = HephIndent.closerIndent(text, closerOffset) ?: return TypedHandlerDelegate.Result.CONTINUE
        val lineStart = HephIndent.lineStart(text, closerOffset)
        if (text.subSequence(lineStart, closerOffset).toString() == indent) return TypedHandlerDelegate.Result.CONTINUE
        document.replaceString(lineStart, closerOffset, indent)
        return TypedHandlerDelegate.Result.CONTINUE
    }
}
