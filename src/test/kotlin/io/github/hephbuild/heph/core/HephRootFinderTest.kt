package io.github.hephbuild.heph.core

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class HephRootFinderTest : BasePlatformTestCase() {
    fun testFindsRoot() {
        val fixture = myFixture.tempDirFixture
        val root = fixture.createFile("repo/.hephconfig").parent
        val rootBuild = fixture.createFile("repo/BUILD")
        val nested = fixture.createFile("repo/a/b/BUILD")
        val outside = fixture.createFile("plain/BUILD")
        fixture.createFile("local/.hephconfig.local")
        val localBuild = fixture.createFile("local/BUILD")

        val cases = mapOf(
            "marker at root" to (rootBuild to root),
            "nested file" to (nested to root),
            "no marker" to (outside to null),
            "only hephconfig.local" to (localBuild to null),
        )
        for ((name, case) in cases) {
            assertEquals(name, case.second, HephRootFinder.find(case.first))
        }
    }

    fun testDeletedFilesHaveNoRoot() {
        val fixture = myFixture.tempDirFixture
        fixture.createFile("gone/.hephconfig")
        val build = fixture.createFile("gone/pkg/BUILD")
        val dir = build.parent
        com.intellij.openapi.application.runWriteAction { fixture.getFile("gone")!!.delete(this) }

        assertFalse(build.isValid)
        assertNull(HephRootFinder.find(build))
        assertNull(HephRootFinder.find(dir))
        assertNull(HephBuildFiles.rootFor(build))
    }
}
