package io.github.hephbuild.heph.lang

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.LanguageFileType
import io.github.hephbuild.heph.HephBundle
import javax.swing.Icon

/** No static name patterns: assigned by [io.github.hephbuild.heph.filetype.HephFileTypeOverrider]. */
object HephFileType : LanguageFileType(HephLanguage) {
    override fun getName(): String = "Heph"

    override fun getDescription(): String = HephBundle.message("filetype.description")

    override fun getDefaultExtension(): String = ""

    override fun getIcon(): Icon = AllIcons.FileTypes.Text
}
