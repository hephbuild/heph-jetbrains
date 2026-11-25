package io.github.hephbuild.heph.core

import com.intellij.execution.configurations.GeneralCommandLine
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Path

class HephCommandLineTest {
    private val bin = Path.of("/usr/local/bin/heph")
    private val root = Path.of("/repo")

    @Test
    fun args() {
        val cases = mapOf(
            "default" to ("tool build-lsp" to listOf("tool", "build-lsp")),
            "quoted" to ("tool \"build lsp\" --x='a b'" to listOf("tool", "build lsp", "--x=a b")),
            "single quotes" to ("--x='a b' c" to listOf("--x=a b", "c")),
            "empty falls back to the default" to ("" to listOf("tool", "build-lsp")),
            "blank falls back to the default" to ("  " to listOf("tool", "build-lsp")),
        )
        for ((name, case) in cases) {
            assertEquals(name, case.second, HephCommandLine.build(bin, case.first, root).parametersList.list)
        }
    }

    @Test
    fun cwdAndEnv() {
        val cmd = HephCommandLine.build(bin, HephCommandLine.DEFAULT_ARGS, root)
        assertEquals(bin.toString(), cmd.exePath)
        assertEquals(root.toFile(), cmd.workDirectory)
        assertEquals(GeneralCommandLine.ParentEnvironmentType.CONSOLE, cmd.parentEnvironmentType)
    }
}
