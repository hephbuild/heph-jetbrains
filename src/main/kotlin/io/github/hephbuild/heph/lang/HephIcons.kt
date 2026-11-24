package io.github.hephbuild.heph.lang

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object HephIcons {
    /** `hephFile_dark.svg` is picked automatically for dark themes. */
    @JvmField
    val FILE: Icon = IconLoader.getIcon("/icons/hephFile.svg", HephIcons::class.java)
}
