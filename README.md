# Heph for JetBrains IDEs

Language support for [heph](https://hephbuild.github.io/) BUILD files, powered by the heph
language server (`heph tool build-lsp`).

- Diagnostics, completion, hover, go to definition and formatting (Reformat Code).
- A `Heph` file type with syntax highlighting, commenting, brace matching and quote handling.
- Completion inserts `()` for functions and `name = "",` for string arguments.

Requires IntelliJ IDEA or GoLand 2025.3 or newer.

## Settings

Settings | Languages & Frameworks | Heph

- **Heph executable**: prefilled with the detected location. Change it to use another binary.
- **Server arguments**: defaults to `tool build-lsp`.
- **Additional file names**: names or globs, separated by semicolons, for build files other than
  `BUILD`, `BUILD.*` and `*.BUILD`.

Files matching these names inside a directory tree containing `.hephconfig` open as `Heph` and
get the language server. Colours: Settings | Editor | Color Scheme | Heph.

## Debugging

Enable debug logging for `#com.intellij.platform.lsp` and `#io.github.hephbuild.heph`
(Help | Diagnostic Tools | Debug Log Settings), then check `idea.log`.

## Development

Requires JDK 21.

```
./gradlew test buildPlugin verifyPlugin
./gradlew runIde
```

Source: https://github.com/hephbuild/heph-jetbrains
