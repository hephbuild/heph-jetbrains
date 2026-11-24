package io.github.hephbuild.heph.core

import com.intellij.util.EnvironmentUtil
import java.io.File
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path

sealed interface HephBinaryResult {
    /** The resolved executable. */
    data class Found(val path: Path) : HephBinaryResult

    sealed interface Failure : HephBinaryResult

    sealed interface NotFound : Failure {
        /** [value] is the configured path or command name as the user typed it. */
        data class Configured(val value: String) : NotFound

        /** [searched] lists the locations looked at, in order, for display. */
        data class AutoDetect(val searched: List<String>) : NotFound
    }

    /** The configured path exists but is not a regular file, e.g. a directory. */
    data class NotAFile(val value: String) : Failure

    /** The configured path is a regular file without the execute permission. */
    data class NotExecutable(val value: String) : Failure

    /** The existence of the configured path could not be determined, e.g. an unreadable parent directory. */
    data class Inaccessible(val value: String) : Failure
}

enum class HephPathState { Present, Absent, Unknown }

class HephBinaryResolver(
    private val findOnPath: (String) -> Path? = { findInPathValue(EnvironmentUtil.getValue("PATH"), it) },
    private val fallbackDirs: List<Path> = defaultFallbackDirs(),
    private val pathState: (Path) -> HephPathState = { probe(it) },
    private val isFile: (Path) -> Boolean = Files::isRegularFile,
    private val isExecutable: (Path) -> Boolean = Files::isExecutable,
    private val home: String? = System.getProperty("user.home"),
) {
    fun resolve(explicit: String): HephBinaryResult {
        val configured = unquote(explicit.trim())
        if (configured.isNotEmpty()) return resolveConfigured(configured)
        val found = CANDIDATE_NAMES.firstNotNullOfOrNull { name ->
            findOnPath(name) ?: fallbackDirs.firstNotNullOfOrNull { dir ->
                dir.resolve(name).takeIf { isFile(it) && isExecutable(it) }
            }
        }
        return if (found != null) HephBinaryResult.Found(found) else HephBinaryResult.NotFound.AutoDetect(searchedLocations())
    }

    private fun searchedLocations(): List<String> = listOf("PATH") + fallbackDirs.map { displayPath(it, home) }

    private fun resolveConfigured(configured: String): HephBinaryResult {
        if (configured != "~" && configured.none { it == '/' || it == '\\' }) {
            val onPath = findOnPath(configured)
            return if (onPath != null) HephBinaryResult.Found(onPath) else HephBinaryResult.NotFound.Configured(configured)
        }
        val path = try {
            Path.of(expandHome(configured))
        } catch (_: InvalidPathException) {
            return HephBinaryResult.NotFound.Configured(configured)
        }
        if (!path.isAbsolute) return HephBinaryResult.NotFound.Configured(configured)
        return when (pathState(path)) {
            HephPathState.Absent -> HephBinaryResult.NotFound.Configured(configured)
            HephPathState.Unknown -> HephBinaryResult.Inaccessible(configured)
            HephPathState.Present -> when {
                !isFile(path) -> HephBinaryResult.NotAFile(configured)
                !isExecutable(path) -> HephBinaryResult.NotExecutable(configured)
                else -> HephBinaryResult.Found(path)
            }
        }
    }

    companion object {
        // TODO: remove "heph1" once v0 is gone.
        val CANDIDATE_NAMES = listOf("heph1", "heph")

        /** First executable regular file called [name] in the [pathValue] directories; unusable entries are skipped. */
        fun findInPathValue(pathValue: String?, name: String): Path? =
            pathValue.orEmpty().split(File.pathSeparator).asSequence()
                .filter { it.isNotEmpty() }
                .mapNotNull { dir -> try { Path.of(dir, name) } catch (_: InvalidPathException) { null } }
                .firstOrNull { Files.isRegularFile(it) && Files.isExecutable(it) }

        /** `~` and `~/...` become the home directory; `~user/...` and everything else stay as they are. */
        fun expandHome(path: String, home: String? = System.getProperty("user.home")): String = when {
            home == null -> path
            path == "~" -> home
            path.startsWith("~/") || path.startsWith("~\\") -> home + path.substring(1)
            else -> path
        }

        /** One pair of matching surrounding single or double quotes is removed. */
        fun unquote(value: String): String =
            if (value.length >= 2 && (value[0] == '"' || value[0] == '\'') && value.last() == value[0]) {
                value.substring(1, value.length - 1)
            } else {
                value
            }

        private fun probe(path: Path): HephPathState = when {
            Files.exists(path) -> HephPathState.Present
            Files.notExists(path) -> HephPathState.Absent
            else -> HephPathState.Unknown
        }

        /** Shows [dir] with the home directory as `~`. */
        fun displayPath(dir: Path, home: String?): String {
            if (home == null) return dir.toString()
            val homePath = try {
                Path.of(home)
            } catch (_: InvalidPathException) {
                return dir.toString()
            }
            if (dir == homePath || !dir.startsWith(homePath)) return dir.toString()
            return "~/" + homePath.relativize(dir).joinToString("/")
        }

        fun defaultFallbackDirs(home: String? = System.getProperty("user.home")): List<Path> = listOfNotNull(
            home?.let { Path.of(it, ".local", "bin") },
            Path.of("/opt/homebrew/bin"),
            Path.of("/usr/local/bin"),
        )
    }
}
