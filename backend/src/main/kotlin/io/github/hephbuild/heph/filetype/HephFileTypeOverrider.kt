package io.github.hephbuild.heph.filetype

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.impl.FileTypeOverrider
import com.intellij.openapi.vfs.VirtualFile
import io.github.hephbuild.heph.core.HephBuildFiles
import io.github.hephbuild.heph.lang.HephFileType

/**
 * Types heph BUILD files as [HephFileType] ahead of name-based mappings (e.g. Bazel's `BUILD`), inside heph repos only.
 * Hot path: name check first, no read action, never asks for a file type.
 */
class HephFileTypeOverrider : FileTypeOverrider {
    override fun getOverriddenFileType(file: VirtualFile): FileType? =
        if (HephBuildFiles.rootFor(file) != null) HephFileType else null
}
