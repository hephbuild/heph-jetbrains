package io.github.hephbuild.heph.core

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path

class HephBinaryResolverTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun file(name: String, executable: Boolean): Path =
        tmp.newFile(name).also { it.setExecutable(executable) }.toPath()

    @Test
    fun explicitValid() {
        val bin = file("heph-ok", true)
        assertEquals(HephBinaryResult.Found(bin), HephBinaryResolver(findOnPath = { null }, fallbacks = emptyList()).resolve(bin.toString()))
    }

    @Test
    fun explicitMissing() {
        val missing = tmp.root.toPath().resolve("nope").toString()
        assertEquals(
            HephBinaryResult.NotFound(missing),
            HephBinaryResolver(findOnPath = { null }, fallbacks = emptyList()).resolve(missing),
        )
    }

    @Test
    fun explicitNotExecutable() {
        val bin = file("heph-noexec", false)
        assertEquals(
            HephBinaryResult.NotExecutable(bin),
            HephBinaryResolver(findOnPath = { null }, fallbacks = emptyList()).resolve(bin.toString()),
        )
    }

    @Test
    fun pathHit() {
        val bin = file("heph-path", true)
        assertEquals(HephBinaryResult.Found(bin), HephBinaryResolver(findOnPath = { bin }, fallbacks = emptyList()).resolve(""))
    }

    @Test
    fun fallbackHit() {
        val bin = file("heph-fallback", true)
        val missing = tmp.root.toPath().resolve("absent")
        assertEquals(
            HephBinaryResult.Found(bin),
            HephBinaryResolver(findOnPath = { null }, fallbacks = listOf(missing, bin)).resolve("  "),
        )
    }

    @Test
    fun noneFound() {
        assertEquals(
            HephBinaryResult.NotFound(null),
            HephBinaryResolver(findOnPath = { null }, fallbacks = emptyList()).resolve(""),
        )
    }

    @Test
    fun explicitDirectoryIsNotExecutableNotMissing() {
        val dir = tmp.newFolder("heph-dir").toPath()
        assertEquals(
            HephBinaryResult.NotExecutable(dir),
            HephBinaryResolver(findOnPath = { null }, fallbacks = emptyList()).resolve(dir.toString()),
        )
    }

    @Test
    fun explicitExpandsLeadingTilde() {
        val bin = file("heph-home", true)
        val oldHome = System.getProperty("user.home")
        System.setProperty("user.home", tmp.root.path)
        try {
            assertEquals(
                HephBinaryResult.Found(bin),
                HephBinaryResolver(findOnPath = { null }, fallbacks = emptyList()).resolve("~/heph-home"),
            )
        } finally {
            System.setProperty("user.home", oldHome)
        }
    }
}
