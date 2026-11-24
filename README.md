# Heph for JetBrains IDEs

[Heph](https://hephbuild.github.io/) support for JetBrains IDEs. Requires IntelliJ IDEA or GoLand 2026.3 or newer.

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

Requires JDK 25, provided by `devenv shell`.

```
./gradlew test buildPlugin verifyPlugin
./gradlew runIde
```

Source: https://github.com/hephbuild/heph-jetbrains
