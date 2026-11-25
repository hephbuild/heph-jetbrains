package io.github.hephbuild.heph.core

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.util.execution.ParametersListUtil
import java.nio.charset.StandardCharsets
import java.nio.file.Path

object HephCommandLine {
    const val DEFAULT_ARGS = "tool build-lsp"

    fun build(binary: Path, args: String, root: Path): GeneralCommandLine =
        GeneralCommandLine(binary.toString())
            .withParameters(ParametersListUtil.parse(args.ifBlank { DEFAULT_ARGS }, false, true))
            .withWorkDirectory(root.toFile())
            .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
            .withCharset(StandardCharsets.UTF_8)
}
