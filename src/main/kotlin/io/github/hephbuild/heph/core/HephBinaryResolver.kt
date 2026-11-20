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
    private val findOnPath: () -> Path? = { PathEnvironmentVariableUtil.findInPath(BINARY_NAME)?.toPath() },
    private val fallbacks: List<Path> = defaultFallbacks(),
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
        findOnPath()?.let { return HephBinaryResult.Found(it) }
        fallbacks.firstOrNull { isFile(it) && isExecutable(it) }?.let { return HephBinaryResult.Found(it) }
        return HephBinaryResult.NotFound(null)
    }

    companion object {
        const val BINARY_NAME = "heph"

        /** `~` and `~/...` become the home directory; `~user/...` and everything else stay as they are. */
        fun expandHome(path: String, home: String? = System.getProperty("user.home")): String = when {
            home == null -> path
            path == "~" -> home
            path.startsWith("~/") || path.startsWith("~\\") -> home + path.substring(1)
            else -> path
        }

        fun defaultFallbacks(): List<Path> = listOf(
            Path.of(System.getProperty("user.home"), ".local", "bin", BINARY_NAME),
            Path.of("/opt/homebrew/bin", BINARY_NAME),
            Path.of("/usr/local/bin", BINARY_NAME),
        )
    }
}
