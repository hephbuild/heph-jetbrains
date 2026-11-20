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

## Settings

Settings | Languages & Frameworks | Heph

- Heph executable: prefilled with the detected location (`PATH` from the shell environment, then
  `~/.local/bin`, `/opt/homebrew/bin`, `/usr/local/bin`); change it to use another binary.
- Server arguments: default `tool build-lsp`. `build-lsp` is not a stable public heph API, so this
  stays editable.
- Additional file names: semicolon separated names or globs for build files that do not match the
  defaults. `BUILD`, `BUILD.*` and `*.BUILD` always match.

The server starts only for build files inside a directory tree containing `.hephconfig`; the
nearest such directory is the workspace root and server working directory. Plain Bazel `BUILD`
files elsewhere are ignored. The first start may be slow if heph downloads its pinned version.

Changing settings restarts running heph servers. A missing binary or a crashed server posts a
notification.

## Heph file type and custom file names

Files whose name matches the defaults or the additional file names from Settings, and that live
under a directory containing `.hephconfig`, open as the `Heph` file type. The same rule decides
where the language server starts, so one setting drives both. After changing the additional file
names, open files are re-typed without reopening them.

The file type is only assigned inside heph repositories. A plain Bazel `BUILD` elsewhere keeps its
own file type (Bazel/Starlark or Plain Text) and starts no server. Inside a heph repository the
plugin takes `BUILD` over from other file types without a "File type conflict" notification.
Creating or deleting a `.hephconfig` re-types the build files below it.

Colours: Settings | Editor | Color Scheme | Heph.

## Debugging

Enable debug logging for `#com.intellij.platform.lsp` (Help | Diagnostic Tools | Debug Log Settings).

## Development

Requires JDK 21.

```
./gradlew test buildPlugin verifyPlugin
./gradlew runIde
```
