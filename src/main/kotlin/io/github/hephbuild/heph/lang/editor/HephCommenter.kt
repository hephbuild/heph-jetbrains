package io.github.hephbuild.heph.lang.editor

import com.intellij.lang.Commenter

class HephCommenter : Commenter {
    override fun getLineCommentPrefix(): String = "# "

    override fun getBlockCommentPrefix(): String? = null

    override fun getBlockCommentSuffix(): String? = null

    override fun getCommentedBlockCommentPrefix(): String? = null

    override fun getCommentedBlockCommentSuffix(): String? = null
}
