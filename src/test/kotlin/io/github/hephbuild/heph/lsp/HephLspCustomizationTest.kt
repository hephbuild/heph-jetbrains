package io.github.hephbuild.heph.lsp

import com.intellij.platform.lsp.api.customization.LspFormattingSupport
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.nio.file.Path

class HephLspCustomizationTest : BasePlatformTestCase() {
    fun testServerAlwaysFormats() {
        val file = myFixture.tempDirFixture.createFile("repo/BUILD")
        val support = HephFormattingSupport()
        val cases = mapOf(
            "ide cannot, server not explicit" to (false to false),
            "ide cannot, server explicit" to (false to true),
            "ide can, server not explicit" to (true to false),
            "ide can, server explicit" to (true to true),
        )
        for ((name, case) in cases) {
            assertTrue(name, support.shouldFormatThisFileExclusivelyByServer(file, case.first, case.second))
        }
    }

    fun testDescriptorUsesHephFormatting() {
        val root = myFixture.tempDirFixture.createFile("repo/.hephconfig").parent
        val descriptor = HephLspClientDescriptor(project, root, Path.of("/usr/local/bin/heph"))
        assertTrue(descriptor.lspCustomization.formattingCustomizer is LspFormattingSupport)
        assertTrue(descriptor.lspCustomization.formattingCustomizer is HephFormattingSupport)
    }

    fun testCompletionWiring() {
        assertTrue(HephLspCustomization().completionCustomizer is HephCompletionSupport)
        assertTrue(HephLspCustomization().formattingCustomizer is HephFormattingSupport)
        val root = myFixture.tempDirFixture.createFile("repo/.hephconfig").parent
        val descriptor = HephLspClientDescriptor(project, root, Path.of("/usr/local/bin/heph"))
        assertTrue(descriptor.lspCustomization.completionCustomizer is HephCompletionSupport)
    }
}
