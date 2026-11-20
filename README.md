# Heph for JetBrains IDEs

Connects JetBrains IDEs to the [heph](https://hephbuild.github.io/) language server
(`heph tool build-lsp`) for BUILD files, and adds a `Heph` file type with syntax highlighting,
a colour settings page, commenting, brace matching and quote handling.

Features come from the server: diagnostics, completion, hover, go to definition and formatting
(Reformat Code).

Completion tweaks in the plugin: function items insert `()` with the caret between the parentheses;
argument names insert `name = ,` with the caret before the comma, and plain `string` arguments insert
`name = "|",` and open the completion popup for the value.

Source: https://github.com/hephbuild/heph-jetbrains

## Supported IDEs

Latest IntelliJ IDEA and GoLand (build 253 / 2025.3 and newer). The plugin uses the IDE's native
LSP support, so IDEs without it (Android Studio, IDEA OSS builds, pre-2025.3) are not supported.

## Install

Build with `./gradlew buildPlugin`, then Settings | Plugins | gear | Install Plugin from Disk and
pick `build/distributions/heph-*.zip`.

## Settings

Settings | Languages & Frameworks | Heph

- Heph executable: empty means auto-detect (`PATH` from the shell environment, then
  `~/.local/bin`, `/opt/homebrew/bin`, `/usr/local/bin`).
- Server arguments: default `tool build-lsp`. `build-lsp` is not a stable public heph API, so this
  stays editable.
- Additional BUILD file names: semicolon separated globs. `BUILD`, `BUILD.*` and `*.BUILD` always match.

The server starts only for BUILD files inside a directory tree containing `.hephconfig`; the
nearest such directory is the workspace root and server working directory. Plain Bazel `BUILD`
files elsewhere are ignored. The first start may be slow if heph downloads its pinned version.

Changing settings restarts running heph servers. Missing binary or a crashed server posts a
notification.

## Heph file type and custom file names such as `BUILD2`

Files whose name matches (`BUILD`, `BUILD.*`, `*.BUILD` plus the additional names from Settings)
and that live under a directory containing `.hephconfig` open as the `Heph` file type. The
same rule decides where the language server starts, so one setting drives both. To use `BUILD2`,
add it to Additional BUILD file names and apply: open files are re-typed without reopening them.

The file type is only assigned inside heph repositories. A plain Bazel `BUILD` elsewhere keeps its
own file type (Bazel/Starlark or Plain Text) and starts no server. Inside a heph repository the
plugin takes `BUILD` over from other file types without a "File type conflict" notification.
Creating or deleting a `.hephconfig` re-types the BUILD files below it.

Colours: Settings | Editor | Color Scheme | Heph.

## Debugging

Enable debug logging for `#com.intellij.platform.lsp` (Help | Diagnostic Tools | Debug Log Settings).

## Development

```
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
./gradlew test buildPlugin verifyPlugin
./gradlew runIde
```

The wrapper jar is not committed; generate it with `gradle wrapper` if missing.
