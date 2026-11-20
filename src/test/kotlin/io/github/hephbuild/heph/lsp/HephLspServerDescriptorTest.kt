package io.github.hephbuild.heph.lsp

import io.github.hephbuild.heph.settings.HephSettings
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.nio.file.Path

class HephLspServerDescriptorTest : BasePlatformTestCase() {
    override fun tearDown() {
        try {
            HephSettings.getInstance().extraFilePatterns = ""
        } finally {
            super.tearDown()
        }
    }

    fun testSupportedFiles() {
        val fixture = myFixture.tempDirFixture
        val root = fixture.createFile("repo/.hephconfig").parent
        val inside = fixture.createFile("repo/pkg/BUILD")
        val insideExtra = fixture.createFile("repo/pkg/BUILD2")
        val insideOther = fixture.createFile("repo/pkg/main.go")
        val outside = fixture.createFile("other/BUILD")
        val descriptor = HephLspServerDescriptor(project, root, Path.of("/usr/local/bin/heph"))

        assertTrue(descriptor.isSupportedFile(inside))
        assertFalse(descriptor.isSupportedFile(insideExtra))
        assertFalse(descriptor.isSupportedFile(insideOther))
        assertFalse(descriptor.isSupportedFile(outside))

        HephSettings.getInstance().extraFilePatterns = "BUILD2"
        assertTrue(descriptor.isSupportedFile(insideExtra))
    }

    fun testNestedRootClaimsOnlyOwnFiles() {
        val fixture = myFixture.tempDirFixture
        val outer = fixture.createFile("repo/.hephconfig").parent
        val inner = fixture.createFile("repo/sub/.hephconfig").parent
        val innerBuild = fixture.createFile("repo/sub/pkg/BUILD")
        val outerBuild = fixture.createFile("repo/pkg/BUILD")
        val bin = Path.of("/usr/local/bin/heph")
        val outerDescriptor = HephLspServerDescriptor(project, outer, bin)
        val innerDescriptor = HephLspServerDescriptor(project, inner, bin)

        assertFalse(outerDescriptor.isSupportedFile(innerBuild))
        assertTrue(innerDescriptor.isSupportedFile(innerBuild))
        assertTrue(outerDescriptor.isSupportedFile(outerBuild))
    }

    fun testLanguageIdAndRoots() {
        val fixture = myFixture.tempDirFixture
        val root = fixture.createFile("repo/.hephconfig").parent
        val build = fixture.createFile("repo/BUILD")
        val descriptor = HephLspServerDescriptor(project, root, Path.of("/usr/local/bin/heph"))

        assertEquals("hephbuild", descriptor.getLanguageId(build))
        assertEquals(listOf(root), descriptor.roots.toList())
    }
}
