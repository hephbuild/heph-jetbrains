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
            HephBinaryResult.NotFound.Configured(missing),
            HephBinaryResolver(findOnPath = { null }, fallbackDirs = emptyList()).resolve(missing),
        )
    }

    @Test
    fun explicitNotExecutable() {
        val bin = file("heph-noexec", false)
        assertEquals(
            HephBinaryResult.NotExecutable(bin.toString()),
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
            HephBinaryResult.NotFound.AutoDetect(listOf("PATH")),
            HephBinaryResolver(findOnPath = { null }, fallbackDirs = emptyList()).resolve(""),
        )
    }

    @Test
    fun explicitDirectoryIsNotAFile() {
        val dir = tmp.newFolder("heph-dir").toPath()
        assertEquals(
            HephBinaryResult.NotAFile(dir.toString()),
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
                home = null,
            )
            val want = case.want?.let {
                val (where, file) = it.split("/")
                HephBinaryResult.Found((if (where == "path") pathDir else fallbackDir).resolve(file))
            } ?: HephBinaryResult.NotFound.AutoDetect(listOf("PATH", fallbackDir.toString()))
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

    @Test
    fun invalidConfiguredPath() {
        val raw = "/tmp/a\u0000b"
        assertEquals(
            HephBinaryResult.NotFound.Configured(raw),
            HephBinaryResolver(findOnPath = { null }, fallbackDirs = emptyList()).resolve(raw),
        )
    }

    @Test
    fun configuredNameWithoutSeparator() {
        val dir = tmp.newFolder().toPath()
        val onPath = executableIn(dir, "heph")
        executableIn(dir, "heph1")
        val requested = mutableListOf<String>()
        val resolver = HephBinaryResolver(
            findOnPath = { n ->
                requested += n
                dir.resolve(n).takeIf { Files.isExecutable(it) }
            },
            fallbackDirs = emptyList(),
        )
        assertEquals(HephBinaryResult.Found(onPath), resolver.resolve("heph"))
        assertEquals(listOf("heph"), requested)
        assertEquals(HephBinaryResult.NotFound.Configured("other"), resolver.resolve("other"))
        assertEquals(HephBinaryResult.Found(dir.resolve("heph1")), resolver.resolve("heph1"))
    }

    @Test
    fun configuredRelativePathWithSeparator() {
        val cases = mapOf(
            "relative" to "bin/heph",
            "dot slash" to "./heph",
            "parent" to "../heph",
            "backslash" to "bin\\heph",
        )
        for ((name, value) in cases) {
            assertEquals(
                name,
                HephBinaryResult.NotFound.Configured(value),
                HephBinaryResolver(findOnPath = { Path.of("/never") }, fallbackDirs = emptyList()).resolve(value),
            )
        }
    }

    @Test
    fun configuredQuotes() {
        val bin = file("quoted-heph", true)
        val resolver = HephBinaryResolver(findOnPath = { null }, fallbackDirs = emptyList())
        val found = HephBinaryResult.Found(bin)
        val cases = mapOf(
            "double quotes" to ("\"$bin\"" to found),
            "single quotes" to ("'$bin'" to found),
            "quotes with outer blanks" to ("  \"$bin\"  " to found),
            "mismatched quotes" to ("'$bin\"" to HephBinaryResult.NotFound.Configured("'$bin\"")),
            "only an opening quote" to ("\"$bin" to HephBinaryResult.NotFound.Configured("\"$bin")),
            "lone quote" to ("\"" to HephBinaryResult.NotFound.Configured("\"")),
            "doubled quotes strip one pair" to ("\"\"$bin\"\"" to HephBinaryResult.NotFound.Configured("\"$bin\"")),
        )
        for ((name, case) in cases) {
            assertEquals(name, case.second, resolver.resolve(case.first))
        }
    }

    @Test
    fun unquote() {
        val cases = mapOf(
            "empty" to ("" to ""),
            "empty quotes" to ("\"\"" to ""),
            "double" to ("\"a\"" to "a"),
            "single" to ("'a'" to "a"),
            "mismatched" to ("'a\"" to "'a\""),
            "inner quote kept" to ("\"a'b\"" to "a'b"),
            "no quotes" to ("a" to "a"),
        )
        for ((name, case) in cases) {
            assertEquals(name, case.second, HephBinaryResolver.unquote(case.first))
        }
    }

    @Test
    fun fallbackDirsWithoutHome() {
        val opt = Path.of("/opt/homebrew/bin")
        val local = Path.of("/usr/local/bin")
        assertEquals(listOf(opt, local), HephBinaryResolver.defaultFallbackDirs(null))
        assertEquals(
            listOf(Path.of("/h", ".local", "bin"), opt, local),
            HephBinaryResolver.defaultFallbackDirs("/h"),
        )
    }

    @Test
    fun configuredFailuresCarryTheTypedValue() {
        val dir = tmp.newFolder("a-dir").toPath()
        val noExec = file("not-exec", false)
        val missing = tmp.root.toPath().resolve("missing").toString()
        val resolver = HephBinaryResolver(findOnPath = { null }, fallbackDirs = emptyList())
        val cases = mapOf(
            "missing" to (missing to HephBinaryResult.NotFound.Configured(missing)),
            "directory" to (dir.toString() to HephBinaryResult.NotAFile(dir.toString())),
            "not executable" to (noExec.toString() to HephBinaryResult.NotExecutable(noExec.toString())),
            "quoted directory" to ("'$dir'" to HephBinaryResult.NotAFile(dir.toString())),
            "quoted not executable" to ("\"$noExec\"" to HephBinaryResult.NotExecutable(noExec.toString())),
            "padded missing" to ("  $missing  " to HephBinaryResult.NotFound.Configured(missing)),
        )
        for ((name, case) in cases) {
            assertEquals(name, case.second, resolver.resolve(case.first))
        }
    }

    @Test
    fun pathStateDecidesMissingFromInaccessible() {
        val bin = file("state-heph", true)
        val cases = mapOf(
            "present" to (HephPathState.Present to HephBinaryResult.Found(bin)),
            "absent" to (HephPathState.Absent to HephBinaryResult.NotFound.Configured(bin.toString())),
            "unknown" to (HephPathState.Unknown to HephBinaryResult.Inaccessible(bin.toString())),
        )
        for ((name, case) in cases) {
            val resolver = HephBinaryResolver(
                findOnPath = { null },
                fallbackDirs = emptyList(),
                pathState = { case.first },
            )
            assertEquals(name, case.second, resolver.resolve(bin.toString()))
        }
    }

    @Test
    fun brokenSymlinkIsNotFound() {
        val link = tmp.root.toPath().resolve("dangling")
        Files.createSymbolicLink(link, tmp.root.toPath().resolve("no-such-target"))
        assertEquals(
            HephBinaryResult.NotFound.Configured(link.toString()),
            HephBinaryResolver(findOnPath = { null }, fallbackDirs = emptyList()).resolve(link.toString()),
        )
    }

    @Test
    fun autoDetectReportsSearchedLocationsWithoutBinaryNames() {
        val resolver = HephBinaryResolver(
            findOnPath = { null },
            fallbackDirs = HephBinaryResolver.defaultFallbackDirs("/h"),
            home = "/h",
        )
        val result = resolver.resolve("") as HephBinaryResult.NotFound.AutoDetect
        assertEquals(listOf("PATH", "~/.local/bin", "/opt/homebrew/bin", "/usr/local/bin"), result.searched)
        for (location in result.searched) {
            for (name in HephBinaryResolver.CANDIDATE_NAMES) {
                assertEquals("$location mentions $name", false, location.contains(name))
            }
        }
    }

    @Test
    fun displayPath() {
        val cases = mapOf(
            "under home" to (Path.of("/h/.local/bin") to "~/.local/bin"),
            "outside home" to (Path.of("/usr/local/bin") to "/usr/local/bin"),
            "home itself" to (Path.of("/h") to "/h"),
            "sibling with the same prefix" to (Path.of("/home2/x") to "/home2/x"),
        )
        for ((name, case) in cases) {
            assertEquals(name, case.second, HephBinaryResolver.displayPath(case.first, "/h"))
        }
        assertEquals("no home", "/h/.local/bin", HephBinaryResolver.displayPath(Path.of("/h/.local/bin"), null))
    }

    @Test
    fun findInPathValue() {
        val sep = java.io.File.pathSeparator
        val plain = tmp.newFolder("plain")
        val noExec = tmp.newFolder("noexec").also { it.resolve("heph").apply { writeText(""); setExecutable(false) } }
        val dirNamedLikeBinary = tmp.newFolder("dirbin").also { it.resolve("heph").mkdir() }
        val good = tmp.newFolder("good").also { it.resolve("heph").apply { writeText(""); setExecutable(true) } }
        val later = tmp.newFolder("later").also { it.resolve("heph").apply { writeText(""); setExecutable(true) } }
        val missing = tmp.root.resolve("missing")

        val cases = mapOf(
            "null path" to (null to null),
            "empty path" to ("" to null),
            "no directory has it" to (plain.path to null),
            "non-executable file is skipped" to (noExec.path to null),
            "directory with the binary's name is skipped" to (dirNamedLikeBinary.path to null),
            "missing and empty entries are skipped" to ("$sep${missing.path}$sep${good.path}" to good.resolve("heph").toPath()),
            "first match in order wins" to ("${noExec.path}$sep${good.path}$sep${later.path}" to good.resolve("heph").toPath()),
        )
        for ((name, case) in cases) {
            assertEquals(name, case.second, HephBinaryResolver.findInPathValue(case.first, "heph"))
        }
    }
}
