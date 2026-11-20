package io.github.hephbuild.heph.lang.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import io.github.hephbuild.heph.lang.HephFileType
import io.github.hephbuild.heph.lang.HephLanguage

class HephFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, HephLanguage) {
    override fun getFileType(): FileType = HephFileType

    override fun toString(): String = "Heph file"
}
