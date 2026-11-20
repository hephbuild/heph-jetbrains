package io.github.hephbuild.heph.filetype

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class HephConfigFileListenerTest : BasePlatformTestCase() {
    fun testIsMarkerEvent() {
        val fixture = myFixture.tempDirFixture
        val config = fixture.createFile("repo/.hephconfig")
        val other = fixture.createFile("repo/BUILD")
        val dir = fixture.findOrCreateDir("repo/sub")
        val tree = fixture.createFile("withconfig/.hephconfig").parent
        val plainDir = fixture.findOrCreateDir("plain")
        val file = fixture.createFile("withconfig/file.txt")
        val copiedParent = fixture.createFile("copied/sub/.hephconfig").parent.parent
        fixture.createFile("copied/plainsub/file.txt")
        val src = this

        val cases = mapOf<String, Pair<VFileEvent, Boolean>>(
            "create marker" to (VFileCreateEvent(src, dir, ".hephconfig", false, null, null, null) to true),
            "create other" to (VFileCreateEvent(src, dir, "BUILD", false, null, null, null) to false),
            "delete marker" to (VFileDeleteEvent(src, config) to true),
            "delete other" to (VFileDeleteEvent(src, other) to false),
            "move marker" to (VFileMoveEvent(src, config, dir) to true),
            "move other" to (VFileMoveEvent(src, other, dir) to false),
            "copy marker" to (VFileCopyEvent(src, config, dir, ".hephconfig") to true),
            "copy other" to (VFileCopyEvent(src, other, dir, "BUILD") to false),
            "rename to marker" to (VFilePropertyChangeEvent(src, other, VirtualFile.PROP_NAME, "x", ".hephconfig") to true),
            "rename from marker" to (VFilePropertyChangeEvent(src, config, VirtualFile.PROP_NAME, ".hephconfig", "x") to true),
            "rename other" to (VFilePropertyChangeEvent(src, other, VirtualFile.PROP_NAME, "BUILD", "BUILD2") to false),
            "create directory with marker" to (
                VFileCreateEvent(src, tree.parent, "withconfig", true, null, null, null) to true
                ),
            "create directory without marker" to (
                VFileCreateEvent(src, plainDir.parent, "plain", true, null, null, null) to false
                ),
            "create file in directory with marker" to (
                VFileCreateEvent(src, tree, "file.txt", false, null, null, null) to false
                ),
            "move directory with marker" to (VFileMoveEvent(src, tree, dir) to true),
            "move directory without marker" to (VFileMoveEvent(src, plainDir, dir) to false),
            "move file next to marker" to (VFileMoveEvent(src, file, dir) to false),
            "copy directory with marker" to (VFileCopyEvent(src, tree, copiedParent, "sub") to true),
            "copy directory without marker" to (VFileCopyEvent(src, plainDir, copiedParent, "plainsub") to false),
            "content change of marker" to (VFileContentChangeEvent(src, config, 0L, 1L) to false),
            "content change of other" to (VFileContentChangeEvent(src, other, 0L, 1L) to false),
        )
        for ((name, case) in cases) {
            assertEquals(name, case.second, HephConfigFileListener.isMarkerEvent(case.first))
        }
    }
}
