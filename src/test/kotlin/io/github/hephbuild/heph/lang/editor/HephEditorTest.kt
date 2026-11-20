package io.github.hephbuild.heph.lang.editor

import com.intellij.codeInsight.highlighting.BraceMatchingUtil
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.options.advanced.AdvancedSettings
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.github.hephbuild.heph.lang.HephFileType
import io.github.hephbuild.heph.lang.lexer.HephTokenTypes

class HephEditorTest : BasePlatformTestCase() {
    private var caretDown = true

    override fun setUp() {
        super.setUp()
        caretDown = AdvancedSettings.getBoolean(CARET_DOWN)
        AdvancedSettings.setBoolean(CARET_DOWN, false)
    }

    override fun tearDown() {
        try {
            AdvancedSettings.setBoolean(CARET_DOWN, caretDown)
        } finally {
            super.tearDown()
        }
    }

    fun testCommentAndUncomment() {
        myFixture.configureByText(HephFileType, "x = 1<caret>\n")
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        myFixture.checkResult("# x = 1<caret>\n")
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        myFixture.checkResult("x = 1<caret>\n")
    }

    fun testAutoCloseAndStepOver() {
        val cases = mapOf(
            "paren" to Triple("<caret>", "(", "(<caret>)"),
            "bracket" to Triple("<caret>", "[", "[<caret>]"),
            "brace" to Triple("<caret>", "{", "{<caret>}"),
            "double quote" to Triple("<caret>", "\"", "\"<caret>\""),
            "single quote" to Triple("<caret>", "'", "'<caret>'"),
            "step over paren" to Triple("(<caret>)", ")", "()<caret>"),
            "triple quote at end of file" to Triple("x = <caret>", "\"\"\"", "x = \"\"\"<caret>\"\"\""),
            "prefixed string r" to Triple("r<caret>", "\"", "r\"<caret>\""),
            "prefixed string b single quote" to Triple("x = b<caret>", "'", "x = b'<caret>'"),
            "prefixed string rb" to Triple("rb<caret>", "\"", "rb\"<caret>\""),
            "step over quote" to Triple("\"a<caret>\"", "\"", "\"a\"<caret>"),
        )
        for ((name, case) in cases) {
            myFixture.configureByText(HephFileType, case.first)
            myFixture.type(case.second)
            val editor = myFixture.editor
            val actual = StringBuilder(editor.document.text).insert(editor.caretModel.offset, "<caret>").toString()
            assertEquals("$name: typing ${case.second} into ${case.first}", case.third, actual)
        }
    }

    fun testClosingTripleQuote() {
        val cases = mapOf(
            "three double quotes" to ("x = \"\"\"" to "\"\"\""),
            "three single quotes" to ("'''" to "'''"),
            "two quotes" to ("\"\"" to null),
            "four quotes" to ("\"\"\"\"" to null),
            "after text" to ("a = \"\"" to null),
            "other chars" to ("abc" to null),
            "too short" to ("\"" to null),
        )
        for ((name, case) in cases) {
            val text = case.first
            assertEquals(name, case.second, HephQuoteHandler.closingQuote(text, text.length)?.toString())
        }
    }

    fun testBracePairs() {
        val pairs = HephBraceMatcher().pairs.map { it.leftBraceType to it.rightBraceType }
        assertEquals(
            listOf(
                HephTokenTypes.LPAR to HephTokenTypes.RPAR,
                HephTokenTypes.LBRACKET to HephTokenTypes.RBRACKET,
                HephTokenTypes.LBRACE to HephTokenTypes.RBRACE,
            ),
            pairs,
        )
    }

    fun testBraceMatchingFindsPartner() {
        val cases = mapOf(
            "paren forward" to Triple("x = <caret>(1, 2)", true, 9),
            "bracket forward" to Triple("x = <caret>[1, [2]]", true, 11),
            "brace forward" to Triple("x = <caret>{\"a\": (1)}", true, 13),
            "paren backward" to Triple("x = (1, 2)<caret>", false, 4),
            "nested bracket backward" to Triple("x = [1, [2]]<caret>", false, 4),
            "brace backward" to Triple("x = {\"a\": (1)}<caret>", false, 4),
        )
        for ((name, case) in cases) {
            myFixture.configureByText(HephFileType, case.first)
            assertEquals(name, case.third, BraceMatchingUtil.getMatchedBraceOffset(myFixture.editor, case.second, myFixture.file))
        }
    }

    fun testNonClosedToken() {
        val cases = mapOf(
            "lone quote" to ("\"" to true),
            "closed double" to ("\"a\"" to false),
            "closed empty" to ("\"\"" to false),
            "unterminated text" to ("\"abc" to true),
            "closed single" to ("'a'" to false),
            "lone single" to ("'" to true),
            "prefix r lone quote" to ("r\"" to true),
            "prefix r closed" to ("r\"a\"" to false),
            "prefix b single lone" to ("b'" to true),
            "prefix rb closed empty" to ("rb\"\"" to false),
            "prefix r unterminated" to ("r\"abc" to true),
            "triple lone" to ("\"\"\"" to true),
            "triple closed" to ("\"\"\"a\"\"\"" to false),
            "prefixed triple unclosed" to ("r\"\"\"a" to true),
        )
        for ((name, case) in cases) {
            assertEquals(name, case.second, HephQuoteHandler.isNonClosedToken(case.first))
        }
    }

    fun testTripleQuoteHelpers() {
        val cases = mapOf(
            "no prefix" to Triple("\"\"\"a\"\"\"", 0, false),
            "inside closing triple first" to Triple("\"\"\"a\"\"\"", 4, true),
            "inside closing triple last" to Triple("\"\"\"a\"\"\"", 6, true),
            "empty triple between" to Triple("\"\"\"\"\"\"", 3, true),
            "empty triple opening" to Triple("\"\"\"\"\"\"", 2, false),
            "unterminated" to Triple("\"\"\"abc", 5, false),
            "plain string" to Triple("\"abc\"", 4, false),
            "prefix closed" to Triple("rb\'\'\'x\'\'\'", 7, true),
        )
        for ((name, case) in cases) {
            assertEquals(name, case.third, HephQuoteHandler.isInClosingTriple(case.first, case.second))
        }
        val prefixes = mapOf("" to 0, "\"a\"" to 0, "r\"a\"" to 1, "rb'a'" to 2, "BR\"a\"" to 2, "rbr\"a\"" to 0, "r" to 0, "rb" to 0)
        for ((token, expected) in prefixes) assertEquals(token, expected, HephQuoteHandler.prefixLength(token))
    }

    // UNVERIFIED: depends on the order in which the platform asks isClosingQuote/isOpeningQuote when typing a quote.
    fun testTypingQuoteInsideEmptyTripleStepsOver() {
        myFixture.configureByText(HephFileType, "x = \"\"\"<caret>\"\"\"")
        myFixture.type('"')
        val editor = myFixture.editor
        val actual = StringBuilder(editor.document.text).insert(editor.caretModel.offset, "<caret>").toString()
        assertEquals("typing a quote inside an empty triple", "x = \"\"\"\"<caret>\"\"", actual)
    }

    fun testNonClosedTripleQuoted() {
        val cases = mapOf(
            "not triple" to ("\"abc\"" to null),
            "single quote string" to ("'a'" to null),
            "empty string" to ("\"\"" to null),
            "three quotes" to ("\"\"\"" to true),
            "opened with text" to ("\"\"\"abc" to true),
            "five quotes" to ("\"\"\"\"\"" to true),
            "closed empty" to ("\"\"\"\"\"\"" to false),
            "closed with text" to ("\"\"\"abc\"\"\"" to false),
            "single quotes closed" to ("\'\'\'a\'\'\'" to false),
            "mixed quotes not closed" to ("\"\"\"abc\'\'\'" to true),
            "prefix r" to ("r\"\"\"" to true),
            "prefix rb closed" to ("rb\'\'\'x\'\'\'" to false),
            "empty" to ("" to null),
        )
        for ((name, case) in cases) {
            assertEquals(name, case.second, HephQuoteHandler.isNonClosedTripleQuoted(case.first))
        }
    }


    private fun actualText(): String {
        val editor = myFixture.editor
        return StringBuilder(editor.document.text).insert(editor.caretModel.offset, "<caret>").toString()
    }

    fun testEnterIndents() {
        val cases = mapOf(
            "empty pair" to ("target(<caret>)" to "target(\n    <caret>\n)"),
            "inside brackets" to ("target(\n    name = \"a\",<caret>\n)" to "target(\n    name = \"a\",\n    <caret>\n)"),
            "nested pair" to ("foo(bar(<caret>))" to "foo(bar(\n    <caret>\n))"),
            "trailing colon" to ("if x:<caret>" to "if x:\n    <caret>"),
            "inside a string keeps default" to ("x = \"(<caret>\"" to null),
        )
        for ((name, case) in cases) {
            myFixture.configureByText(HephFileType, case.first)
            myFixture.performEditorAction(IdeActions.ACTION_EDITOR_ENTER)
            if (case.second != null) assertEquals("$name: Enter in ${case.first}", case.second, actualText())
            else assertFalse("$name: expected default behaviour, got ${actualText()}", actualText().contains("\n    "))
        }
    }

    // Regression: a platform delegate (EnterBetweenBracesHandler) used to insert the newline first, giving four lines.
    fun testEnterBetweenParenthesesGivesExactlyThreeLines() {
        val cases = mapOf(
            "empty call" to ("target(<caret>)" to "target(\n    <caret>\n)"),
            "empty list" to ("x = [<caret>]" to "x = [\n    <caret>\n]"),
            "empty dict" to ("x = {<caret>}" to "x = {\n    <caret>\n}"),
            "indented call" to ("    foo(<caret>)" to "    foo(\n        <caret>\n    )"),
        )
        for ((name, case) in cases) {
            myFixture.configureByText(HephFileType, case.first)
            myFixture.performEditorAction(IdeActions.ACTION_EDITOR_ENTER)
            val actual = actualText()
            assertEquals("$name: Enter in ${case.first}, got ${actual.replace("\n", "\\n")}", case.second, actual)
            assertEquals("$name: line count", 3, myFixture.editor.document.lineCount)
        }
    }

    fun testEnterIsUntouchedInOtherLanguages() {
        myFixture.configureByText("plain.txt", "foo(<caret>)")
        myFixture.performEditorAction(IdeActions.ACTION_EDITOR_ENTER)
        assertFalse("plain text must not be indented: ${actualText()}", actualText().contains("    "))
    }

    fun testTypingCloserDedents() {
        val cases = mapOf(
            "closer on a blank indented line" to ("foo(\n    <caret>" to ")" to "foo(\n)<caret>"),
            "nested closer" to ("foo(\n    bar(\n        <caret>" to ")" to "foo(\n    bar(\n    )<caret>"),
            "line with content is untouched" to ("foo(\n    x <caret>" to ")" to "foo(\n    x )<caret>"),
            "no opener is untouched" to ("    <caret>" to ")" to "    )<caret>"),
        )
        for ((name, case) in cases) {
            myFixture.configureByText(HephFileType, case.first.first)
            myFixture.type(case.first.second)
            assertEquals("$name: typing ${case.first.second} into ${case.first.first}", case.second, actualText())
        }
    }

    private companion object {
        const val CARET_DOWN = "editor.comment.line.caret.down"
    }
}
