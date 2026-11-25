package io.github.hephbuild.heph.filetype

import com.intellij.ide.IconProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import io.github.hephbuild.heph.core.HephBuildFiles
import io.github.hephbuild.heph.lang.HephIcons
import javax.swing.Icon

/**
 * Keeps the Heph icon on heph BUILD files next to icon packs. In remote development the host sends the pack's icon
 * to a client that has no copy of the pack, which renders it empty.
 */
class HephIconProvider : IconProvider(), DumbAware {
    override fun getIcon(element: PsiElement, flags: Int): Icon? {
        val file = (element as? PsiFile)?.virtualFile ?: return null
        return if (HephBuildFiles.rootFor(file) != null) HephIcons.FILE else null
    }
}
