package io.github.hephbuild.heph.filetype

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.ui.UIUtil
import io.github.hephbuild.heph.lang.HephFileType
import java.util.concurrent.atomic.AtomicInteger

class HephFileTypeRefresherTest : BasePlatformTestCase() {
    private val original = HephFileTypeRefresher.afterRefresh

    override fun tearDown() {
        try {
            HephFileTypeRefresher.afterRefresh = original
        } finally {
            super.tearDown()
        }
    }

    fun testRequestsAreCoalescedIntoOneRefreshAndOneFollowUp() {
        val calls = AtomicInteger()
        HephFileTypeRefresher.afterRefresh = { calls.incrementAndGet() }

        HephFileTypeRefresher.refresh()
        HephFileTypeRefresher.refresh()
        HephFileTypeRefresher.refresh()
        UIUtil.dispatchAllInvocationEvents()

        assertEquals(1, calls.get())
    }

    fun testCreatingAndDeletingTheMarkerRetypesAndNudgesServers() {
        val fixture = myFixture.tempDirFixture
        val build = fixture.createFile("late/BUILD")
        myFixture.openFileInEditor(build)
        assertTrue(build.fileType != HephFileType)
        val calls = AtomicInteger()
        HephFileTypeRefresher.afterRefresh = { calls.incrementAndGet() }

        val marker = fixture.createFile("late/.hephconfig")
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(HephFileType, build.fileType)
        assertTrue("servers nudged after the marker appeared", calls.get() > 0)

        val before = calls.get()
        com.intellij.openapi.application.runWriteAction { marker.delete(this) }
        UIUtil.dispatchAllInvocationEvents()
        assertTrue(build.fileType != HephFileType)
        assertTrue("servers nudged after the marker went", calls.get() > before)
    }
}
