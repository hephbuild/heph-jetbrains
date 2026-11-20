package io.github.hephbuild.heph.lang.parser

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.github.hephbuild.heph.lang.HephFileType
import io.github.hephbuild.heph.lang.psi.HephFile

class HephParserDefinitionTest : BasePlatformTestCase() {
    fun testFlatParse() {
        val cases = mapOf(
            "empty" to "",
            "target call" to "target(name = 'x', deps = [\"//a:b\"])\n",
            "comments only" to "# one\n# two\n",
            "unterminated string" to "x = \"abc\ny = 1\n",
            "unterminated triple string" to "x = \"\"\"abc\ndef",
            "bad characters" to "! \\ \$ ?",
            "unbalanced brackets" to "((([",
        )
        for ((name, text) in cases) {
            val file = myFixture.configureByText(HephFileType, text)
            assertTrue(name, file is HephFile)
            assertNull(name, PsiTreeUtil.findChildOfType(file, PsiErrorElement::class.java))
            assertEquals(name, text, file.text)
            assertEquals(name, HephFileType, file.fileType)
        }
    }
}
