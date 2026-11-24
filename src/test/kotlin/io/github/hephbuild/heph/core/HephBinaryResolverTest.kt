package io.github.hephbuild.heph.core

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.Path

class HephBinaryResolverTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun file(name: String, executable: Boolean): Path =
        tmp.newFile(name).also { it.setExecutable(executable) }.toPath()

    @Test
    fun explicitValid() {
        val bin = file("heph-ok", true)
        assertEquals(HephBinaryResult.Found(bin), HephBinaryResolver(findOnPath = { null }, fallbackDirs = emptyList()).resolve(bin.toString()))
    }

    @Test
    fun explicitMissing() {
        val missing = tmp.root.toPath().resolve("nope").toString()
        assertEquals(
            HephBinaryResult.NotFound(missing),
            HephBinaryResolver(findOnPath = { null }, fallbackDirs = emptyList()).resolve(missing),
        )
    }

    @Test
    fun explicitNotExecutable() {
        val bin = file("heph-noexec", false)
        assertEquals(
            HephBinaryResult.NotExecutable(bin),
            HephBinaryResolver(findOnPath = { null }, fallbackDirs = emptyList()).resolve(bin.toString()),
        )
    }

    @Test
    fun pathHit() {
        val bin = file("heph-path", true)
        assertEquals(HephBinaryResult.Found(bin), HephBinaryResolver(findOnPath = { bin }, fallbackDirs = emptyList()).resolve(""))
    }

    @Test
    fun fallbackHit() {
        val dir = tmp.newFolder("fb").toPath()
        val bin = dir.resolve("heph").also { Files.createFile(it); it.toFile().setExecutable(true) }
        val missing = tmp.root.toPath().resolve("absent")
        assertEquals(
            HephBinaryResult.Found(bin),
            HephBinaryResolver(findOnPath = { null }, fallbackDirs = listOf(missing, dir)).resolve("  "),
        )
    }

    @Test
    fun noneFound() {
        assertEquals(
            HephBinaryResult.NotFound(null),
            HephBinaryResolver(findOnPath = { null }, fallbackDirs = emptyList()).resolve(""),
        )
    }

    @Test
    fun explicitDirectoryIsNotExecutableNotMissing() {
        val dir = tmp.newFolder("heph-dir").toPath()
        assertEquals(
            HephBinaryResult.NotExecutable(dir),
            HephBinaryResolver(findOnPath = { null }, fallbackDirs = emptyList()).resolve(dir.toString()),
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
                HephBinaryResolver(findOnPath = { null }, fallbackDirs = emptyList()).resolve("~/heph-home"),
            )
        } finally {
            System.setProperty("user.home", oldHome)
        }
    }

    @Test
    fun expandHome() {
        val cases = mapOf(
            "tilde alone" to ("~" to "/home/me"),
            "tilde slash path" to ("~/x" to "/home/me/x"),
            "tilde slash nested" to ("~/bin/heph" to "/home/me/bin/heph"),
            "other user is untouched" to ("~other/x" to "~other/x"),
            "tilde inside is untouched" to ("/a/~/b" to "/a/~/b"),
            "no tilde" to ("/usr/bin/heph" to "/usr/bin/heph"),
            "relative" to ("bin/heph" to "bin/heph"),
            "empty" to ("" to ""),
        )
        for ((name, case) in cases) {
            assertEquals(name, case.second, HephBinaryResolver.expandHome(case.first, "/home/me"))
        }
        assertEquals("no home", "~/x", HephBinaryResolver.expandHome("~/x", null))
    }

    private fun executableIn(dir: Path, name: String): Path =
        dir.resolve(name).also { Files.createFile(it); it.toFile().setExecutable(true) }

    @Test
    fun lookupOrder() {
        data class Case(val pathNames: Set<String>, val dirNames: Set<String>, val want: String?)
        val cases = mapOf(
            "both on path" to Case(setOf("heph1", "heph"), emptySet(), "path/heph1"),
            "only first name on path" to Case(setOf("heph1"), emptySet(), "path/heph1"),
            "only second name on path" to Case(setOf("heph"), emptySet(), "path/heph"),
            "both in fallback dir" to Case(emptySet(), setOf("heph1", "heph"), "dir/heph1"),
            "first name only in fallback dir, second on path" to Case(setOf("heph"), setOf("heph1"), "dir/heph1"),
            "second name on path before fallback dir" to Case(setOf("heph"), setOf("heph"), "path/heph"),
            "neither" to Case(emptySet(), emptySet(), null),
        )
        for ((name, case) in cases) {
            val pathDir = tmp.newFolder().toPath()
            val fallbackDir = tmp.newFolder().toPath()
            case.pathNames.forEach { executableIn(pathDir, it) }
            case.dirNames.forEach { executableIn(fallbackDir, it) }
            val resolver = HephBinaryResolver(
                findOnPath = { n -> pathDir.resolve(n).takeIf { Files.isExecutable(it) } },
                fallbackDirs = listOf(fallbackDir),
            )
            val want = case.want?.let {
                val (where, file) = it.split("/")
                HephBinaryResult.Found((if (where == "path") pathDir else fallbackDir).resolve(file))
            } ?: HephBinaryResult.NotFound(null)
            assertEquals(name, want, resolver.resolve(""))
        }
    }

    @Test
    fun explicitPathIgnoresLookupOrder() {
        val dir = tmp.newFolder().toPath()
        val first = executableIn(dir, "heph1")
        val second = executableIn(dir, "heph")
        val resolver = HephBinaryResolver(findOnPath = { first }, fallbackDirs = listOf(dir))
        assertEquals(HephBinaryResult.Found(second), resolver.resolve(second.toString()))
    }
}
