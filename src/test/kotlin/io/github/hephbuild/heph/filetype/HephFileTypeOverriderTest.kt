package io.github.hephbuild.heph.filetype

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.ui.UIUtil
import io.github.hephbuild.heph.lang.HephFileType
import io.github.hephbuild.heph.settings.HephSettings

class HephFileTypeOverriderTest : BasePlatformTestCase() {
    private val overrider = HephFileTypeOverrider()

    override fun tearDown() {
        try {
            HephSettings.getInstance().extraFilePatterns = ""
        } finally {
            super.tearDown()
        }
    }

    fun testOverride() {
        val fixture = myFixture.tempDirFixture
        fixture.createFile("repo/.hephconfig")
        val cases = mapOf(
            "BUILD in repo" to (fixture.createFile("repo/pkg/BUILD") to true),
            "BUILD at repo root" to (fixture.createFile("repo/BUILD") to true),
            "x.BUILD" to (fixture.createFile("repo/pkg/x.BUILD") to true),
            "BUILD.foo" to (fixture.createFile("repo/pkg/BUILD.foo") to true),
            "BUILD2 without extra" to (fixture.createFile("repo/pkg/BUILD2") to false),
            "other file" to (fixture.createFile("repo/pkg/main.go") to false),
            "BUILD outside repo" to (fixture.createFile("other/BUILD") to false),
        )
        fixture.createFile("local/.hephconfig.local")
        val localBuild = fixture.createFile("local/BUILD")
        for ((name, case) in cases) {
            assertEquals(name, if (case.second) HephFileType else null, overrider.getOverriddenFileType(case.first))
        }
        assertNull("only .hephconfig.local", overrider.getOverriddenFileType(localBuild))
        assertNull("directory", overrider.getOverriddenFileType(fixture.findOrCreateDir("repo/BUILD.d")))
    }

    fun testExtraPatternAppliesToOverrider() {
        val fixture = myFixture.tempDirFixture
        fixture.createFile("repo/.hephconfig")
        val build2 = fixture.createFile("repo/pkg/BUILD2")
        assertNull(overrider.getOverriddenFileType(build2))

        HephSettings.getInstance().extraFilePatterns = "BUILD2"
        assertEquals(HephFileType, overrider.getOverriddenFileType(build2))
    }

    fun testRefreshRetypesFileAfterSettingChange() {
        val fixture = myFixture.tempDirFixture
        fixture.createFile("repo/.hephconfig")
        val build2 = fixture.createFile("repo/pkg/BUILD2")
        myFixture.openFileInEditor(build2)
        assertTrue(build2.fileType != HephFileType)

        HephSettings.getInstance().extraFilePatterns = "BUILD2"
        HephFileTypeRefresher.refreshNow()
        assertEquals(HephFileType, build2.fileType)
        assertEquals(HephFileType, myFixture.file.fileType)
    }

    fun testCreatingHephconfigRetypesExistingBuild() {
        val fixture = myFixture.tempDirFixture
        val build = fixture.createFile("late/pkg/BUILD")
        assertTrue(build.fileType != HephFileType)

        fixture.createFile("late/.hephconfig")
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(HephFileType, build.fileType)
    }

    fun testFileTypeIntegration() {
        val fixture = myFixture.tempDirFixture
        fixture.createFile("repo/.hephconfig")
        assertEquals(HephFileType, fixture.createFile("repo/pkg/BUILD").fileType)
    }
}
