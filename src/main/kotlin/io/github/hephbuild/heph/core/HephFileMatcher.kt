package io.github.hephbuild.heph.core

/** Matches heph BUILD file names: built-in defaults plus user globs (`*` and `?`). */
class HephFileMatcher(extraPatterns: String) {
    private val regexes: List<Regex> =
        (DEFAULT_PATTERNS + split(extraPatterns)).map(::globToRegex)

    fun matches(fileName: String): Boolean = regexes.any { it.matches(fileName) }

    companion object {
        val DEFAULT_PATTERNS = listOf("BUILD", "BUILD.*", "*.BUILD")

        fun split(patterns: String): List<String> =
            patterns.split(';', ',', '\n', '\r').map { it.trim() }.filter { it.isNotEmpty() }

        private fun globToRegex(glob: String): Regex = buildString {
            for (c in glob) {
                when (c) {
                    '*' -> append(".*")
                    '?' -> append('.')
                    else -> append(Regex.escape(c.toString()))
                }
            }
        }.toRegex()
    }
}
