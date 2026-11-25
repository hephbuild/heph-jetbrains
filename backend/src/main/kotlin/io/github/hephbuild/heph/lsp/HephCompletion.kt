package io.github.hephbuild.heph.lsp

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.platform.lsp.api.customization.LspCompletionSupport
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionItemKind

/** Pure decisions for function completions: the heph server sends no insert text or snippet for them. */
object HephCompletion {
    /** Functions and methods, plus the `load` keyword, are called with parentheses. */
    fun isCallable(kind: CompletionItemKind?, label: String?): Boolean =
        kind == CompletionItemKind.Function ||
            kind == CompletionItemKind.Method ||
            (kind == CompletionItemKind.Keyword && label == "load")

    /** The first character at or after [offset] that is not a space or tab, or null at the end of the text. */
    fun nextSignificantChar(text: CharSequence, offset: Int): Char? {
        var i = offset
        while (i < text.length && (text[i] == ' ' || text[i] == '\t')) i++
        return text.getOrNull(i)
    }

    /**
     * The type part of a field's `detail` (`<call>: <type>[ (required)]`), or null when there is none.
     * Only the first `": "` splits, so types such as `struct(required: bool)` stay whole.
     */
    fun fieldType(detail: String?): String? {
        if (detail == null) return null
        val separator = detail.indexOf(": ")
        if (separator < 0) return null
        var type = detail.substring(separator + 2).trim()
        if (type.endsWith("(required)")) type = type.removeSuffix("(required)").trim()
        return type.ifEmpty { null }
    }

    /** Plain string fields get `""`; unions, lists, maps, bools and structs do not. */
    fun isStringField(kind: CompletionItemKind?, detail: String?): Boolean =
        kind == CompletionItemKind.Field && fieldType(detail) == "string"

    /** What to insert at the tail offset of an argument-name completion and what to do afterwards. */
    data class FieldEdit(val insertText: String, val caretFromTail: Int, val schedulePopup: Boolean)

    /**
     * After the platform inserted `name = `: a plain string field gets `"",` with the caret between the quotes and a
     * completion popup; any other type gets `,` with the caret before it. Null means leave the text alone: not a
     * field, a value or `=` already follows, or there is nothing to add.
     */
    fun fieldEdit(kind: CompletionItemKind?, detail: String?, nextChar: Char?): FieldEdit? {
        if (kind != CompletionItemKind.Field) return null
        if (nextChar == '=' || nextChar == '"' || nextChar == '\'') return null
        val comma = if (nextChar == ',') "" else ","
        if (isStringField(kind, detail)) return FieldEdit("\"\"" + comma, 1, true)
        if (comma.isEmpty()) return null
        return FieldEdit(comma, 0, false)
    }

    /** [fieldEdit] applied to [text] at [tail]: the new text and caret offset, or null if nothing changes. */
    fun applyFieldEdit(text: String, tail: Int, kind: CompletionItemKind?, detail: String?): Pair<String, Int>? {
        val edit = fieldEdit(kind, detail, nextSignificantChar(text, tail)) ?: return null
        return (text.substring(0, tail) + edit.insertText + text.substring(tail)) to (tail + edit.caretFromTail)
    }

    fun shouldAppendParens(kind: CompletionItemKind?, label: String?, nextChar: Char?): Boolean =
        isCallable(kind, label) && nextChar != '('

    /** The text with `()` inserted at [tail] and the caret offset between the parentheses, or null if not needed. */
    fun parensEdit(text: String, tail: Int): Pair<String, Int>? {
        if (nextSignificantChar(text, tail) == '(') return null
        return (text.substring(0, tail) + "()" + text.substring(tail)) to (tail + 1)
    }
}

class HephCompletionSupport : LspCompletionSupport() {
    /** Wraps the platform's lookup element so icons, documentation and the LSP text edits keep working. */
    override fun createLookupElement(parameters: CompletionParameters, item: CompletionItem): LookupElement? {
        val base = super.createLookupElement(parameters, item)
        if (base !is LookupElementBuilder) return base
        return when {
            HephCompletion.isCallable(item.kind, item.label) -> base.withInsertHandler(ParenthesesInsertHandler)
            item.kind == CompletionItemKind.Field -> base.withInsertHandler(FieldInsertHandler(item.kind, item.detail))
            else -> base
        }
    }

    /** Runs after the platform applied the item's `name = ` edit, so the tail offset is right after the `= `. */
    private class FieldInsertHandler(
        private val kind: CompletionItemKind?,
        private val detail: String?,
    ) : InsertHandler<LookupElement> {
        override fun handleInsert(context: InsertionContext, item: LookupElement) {
            val document = context.document
            val tail = context.tailOffset
            val edit = HephCompletion.fieldEdit(kind, detail, HephCompletion.nextSignificantChar(document.charsSequence, tail))
                ?: return
            document.insertString(tail, edit.insertText)
            context.editor.caretModel.moveToOffset(tail + edit.caretFromTail)
            if (edit.schedulePopup) AutoPopupController.getInstance(context.project).scheduleAutoPopup(context.editor)
        }
    }

    /**
     * Runs after the platform applied the item's own edits (it calls the delegate's handler right after them), so
     * the tail offset is the end of the inserted name.
     */
    private object ParenthesesInsertHandler : InsertHandler<LookupElement> {
        override fun handleInsert(context: InsertionContext, item: LookupElement) {
            val document = context.document
            val tail = context.tailOffset
            if (HephCompletion.nextSignificantChar(document.charsSequence, tail) == '(') return
            document.insertString(tail, "()")
            context.editor.caretModel.moveToOffset(tail + 1)
        }
    }
}
