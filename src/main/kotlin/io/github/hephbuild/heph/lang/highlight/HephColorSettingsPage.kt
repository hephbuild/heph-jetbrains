package io.github.hephbuild.heph.lang.highlight

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import io.github.hephbuild.heph.HephBundle
import io.github.hephbuild.heph.lang.HephFileType
import javax.swing.Icon

class HephColorSettingsPage : ColorSettingsPage {
    override fun getIcon(): Icon = HephFileType.icon

    override fun getHighlighter(): SyntaxHighlighter = HephSyntaxHighlighter()

    override fun getDemoText(): String = DEMO

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? = null

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = DESCRIPTORS.toTypedArray()

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getDisplayName(): String = HephFileType.name

    companion object {
        private val DESCRIPTOR_KEYS: List<Pair<String, TextAttributesKey>> = listOf(
            "color.comment" to HephSyntaxHighlighter.COMMENT,
            "color.string" to HephSyntaxHighlighter.STRING,
            "color.number" to HephSyntaxHighlighter.NUMBER,
            "color.keyword" to HephSyntaxHighlighter.KEYWORD,
            "color.builtin" to HephSyntaxHighlighter.BUILTIN,
            "color.identifier" to HephSyntaxHighlighter.IDENTIFIER,
            "color.parentheses" to HephSyntaxHighlighter.PARENTHESES,
            "color.brackets" to HephSyntaxHighlighter.BRACKETS,
            "color.braces" to HephSyntaxHighlighter.BRACES,
            "color.dot" to HephSyntaxHighlighter.DOT,
            "color.comma" to HephSyntaxHighlighter.COMMA,
            "color.semicolon" to HephSyntaxHighlighter.SEMICOLON,
            "color.operator" to HephSyntaxHighlighter.OPERATOR,
        )

        private val DESCRIPTORS: List<AttributesDescriptor> =
            DESCRIPTOR_KEYS.map { (key, attributes) -> AttributesDescriptor(HephBundle.message(key), attributes) }

        private val DEMO = listOf(
            "# Heph file",
            "load(\"//tools:defs\", \"go_lib\")",
            "",
            "target(",
            "    name = \"app\",",
            "    driver = 'bash',",
            "    run = [\"echo hello\", \"\"\"docstring\"\"\"],",
            "    deps = {\"src\": glob(\"*.go\"), \"n\": 0x1F + 2.5e3},",
            "    cache = True,",
            ")",
            "",
            "def helper(x, y = None):",
            "    return [i for i in range(10) if i % 2 == 0 and not x.target]",
            "",
        ).joinToString("\n")
    }
}
