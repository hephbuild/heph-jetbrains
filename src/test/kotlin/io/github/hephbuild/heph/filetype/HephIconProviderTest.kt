package io.github.hephbuild.heph.filetype

import com.intellij.psi.PsiManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.github.hephbuild.heph.lang.HephIcons

class HephIconProviderTest : BasePlatformTestCase() {
    private val provider = HephIconProvider()

    fun testIconOnlyForHephBuildFiles() {
        val fixture = myFixture.tempDirFixture
        fixture.createFile("repo/.hephconfig")
        val manager = PsiManager.getInstance(project)
        val cases = mapOf(
            "BUILD in repo" to (fixture.createFile("repo/pkg/BUILD") to true),
            "other file" to (fixture.createFile("repo/pkg/main.go") to false),
            "BUILD outside repo" to (fixture.createFile("other/BUILD") to false),
        )
        for ((name, case) in cases) {
            val icon = provider.getIcon(manager.findFile(case.first)!!, 0)
            assertEquals(name, if (case.second) HephIcons.FILE else null, icon)
        }
        assertNull("directory", provider.getIcon(manager.findDirectory(fixture.findOrCreateDir("repo"))!!, 0))
    }
}
