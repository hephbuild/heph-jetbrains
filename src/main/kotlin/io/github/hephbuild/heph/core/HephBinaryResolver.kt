package io.github.hephbuild.heph.core

import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import java.nio.file.Files
import java.nio.file.Path

sealed interface HephBinaryResult {
    data class Found(val path: Path) : HephBinaryResult

    /** [explicit] is the configured path, or null when auto-detection found nothing. */
    data class NotFound(val explicit: String?) : HephBinaryResult

    data class NotExecutable(val path: Path) : HephBinaryResult
}

class HephBinaryResolver(
    private val findOnPath: (String) -> Path? = { PathEnvironmentVariableUtil.findInPath(it)?.toPath() },
    private val fallbackDirs: List<Path> = defaultFallbackDirs(),
    private val exists: (Path) -> Boolean = Files::exists,
    private val isFile: (Path) -> Boolean = Files::isRegularFile,
    private val isExecutable: (Path) -> Boolean = Files::isExecutable,
) {
    fun resolve(explicit: String): HephBinaryResult {
        val configured = explicit.trim()
        if (configured.isNotEmpty()) {
            val path = Path.of(expandHome(configured))
            return when {
                !exists(path) -> HephBinaryResult.NotFound(configured)
                !isFile(path) || !isExecutable(path) -> HephBinaryResult.NotExecutable(path)
                else -> HephBinaryResult.Found(path)
            }
        }
        for (name in CANDIDATE_NAMES) {
            findOnPath(name)?.let { return HephBinaryResult.Found(it) }
            fallbackDirs.map { it.resolve(name) }.firstOrNull { isFile(it) && isExecutable(it) }
                ?.let { return HephBinaryResult.Found(it) }
        }
        return HephBinaryResult.NotFound(null)
    }

    companion object {
        // TODO: remove "heph1" once v0 is gone.
        val CANDIDATE_NAMES = listOf("heph1", "heph")

        /** `~` and `~/...` become the home directory; `~user/...` and everything else stay as they are. */
        fun expandHome(path: String, home: String? = System.getProperty("user.home")): String = when {
            home == null -> path
            path == "~" -> home
            path.startsWith("~/") || path.startsWith("~\\") -> home + path.substring(1)
            else -> path
        }

        fun defaultFallbackDirs(): List<Path> = listOf(
            Path.of(System.getProperty("user.home"), ".local", "bin"),
            Path.of("/opt/homebrew/bin"),
            Path.of("/usr/local/bin"),
        )
    }
}
