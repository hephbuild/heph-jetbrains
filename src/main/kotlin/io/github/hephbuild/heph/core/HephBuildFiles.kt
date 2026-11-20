package io.github.hephbuild.heph.core

import com.intellij.openapi.vfs.VirtualFile
import io.github.hephbuild.heph.settings.HephSettings

object HephBuildFiles {
    /** Heph root of [file] when it is a heph BUILD file (name matches and a `.hephconfig` ancestor exists), else null. */
    fun rootFor(file: VirtualFile): VirtualFile? {
        if (!file.isValid || file.isDirectory || file.parent == null) return null
        if (!HephSettings.getInstance().fileMatcher.matches(file.name)) return null
        return HephRootFinder.find(file)
    }
}
