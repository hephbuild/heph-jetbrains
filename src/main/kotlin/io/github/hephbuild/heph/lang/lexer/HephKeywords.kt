package io.github.hephbuild.heph.lang.lexer

object HephKeywords {
    // Starlark keywords and reserved words: https://github.com/bazelbuild/starlark/blob/master/spec.md#lexical-elements
    val KEYWORDS: Set<String> = setOf(
        "and", "break", "continue", "def", "elif", "else", "for", "if", "in", "lambda", "load", "not", "or", "pass", "return",
        "as", "assert", "async", "await", "class", "del", "except", "finally", "from", "global", "import", "is",
        "nonlocal", "raise", "try", "while", "with", "yield",
        "True", "False", "None",
    )

    // Starlark universe: https://github.com/bazelbuild/starlark/blob/master/spec.md#built-in-constants-and-functions
    // Heph build file API: https://hephbuild.github.io/docs/plugins/buildfile
    val BUILTINS: Set<String> = setOf(
        "abs", "all", "any", "bool", "bytes", "chr", "dict", "dir", "enumerate", "fail", "float", "getattr", "hasattr",
        "hash", "int", "len", "list", "max", "min", "ord", "print", "range", "repr", "reversed", "set", "sorted", "str",
        "tuple", "type", "zip",
        "target", "file", "glob", "query", "struct", "get_pkg", "provider_state", "heph",
    )
}
