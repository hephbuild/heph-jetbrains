package io.github.hephbuild.heph.lang.editor

import com.intellij.application.options.CodeStyle
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiFile
import io.github.hephbuild.heph.lang.psi.HephFile

/**
 * Smart indent on Enter for flat-PSI Heph files (no formatter is registered on purpose). The edit is done here and
 * [EnterHandlerDelegate.Result.Stop] skips the platform's newline and indent step; other languages are untouched.
 */
class HephEnterHandler : EnterHandlerDelegateAdapter() {
    override fun preprocessEnter(
        file: PsiFile,
        editor: Editor,
        caretOffset: Ref<Int>,
        caretAdvance: Ref<Int>,
        dataContext: DataContext,
        originalHandler: EditorActionHandler?,
    ): EnterHandlerDelegate.Result {
        if (file !is HephFile) return EnterHandlerDelegate.Result.Continue
        val document = editor.document
        val caret = editor.caretModel.currentCaret
        val offset = caret.offset
        val edit = HephIndent.enter(document.charsSequence, offset, indentUnit(file))
            ?: return EnterHandlerDelegate.Result.Continue

        val text = document.charsSequence
        var end = offset
        while (end < text.length && (text[end] == ' ' || text[end] == '\t')) end++
        document.replaceString(offset, end, edit.text)
        caret.moveToOffset(offset + edit.caretInText)
        editor.scrollingModel.scrollToCaret(ScrollType.RELATIVE)
        return EnterHandlerDelegate.Result.Stop
    }

    private fun indentUnit(file: PsiFile): String {
        val options = CodeStyle.getIndentOptions(file)
        return if (options.USE_TAB_CHARACTER) "\t" else " ".repeat(options.INDENT_SIZE.coerceAtLeast(1))
    }
}
