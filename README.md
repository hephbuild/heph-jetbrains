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

## Remote development

Install the plugin on both the host and the client. The language server runs on the host, which
resolves `heph` from the IDE process `PATH`; if it is not found, set the full path in the settings.

File icon packs installed on the host (Catppuccin Icons, Atom Material Icons, ...) can leave the
Heph file icon blank on the client, as their icons are not available there. Disable them if so.

## Debugging

Enable debug logging for `#com.intellij.platform.lsp` and `#io.github.hephbuild.heph`
(Help | Diagnostic Tools | Debug Log Settings), then check `idea.log`.

## Development

Requires JDK 25, provided by `devenv shell`.

```
./gradlew test buildPlugin verifyPlugin
./gradlew runIde
./gradlew runIdeSplit
```

`runIdeSplit` and `runIdeSplitGoLand` start a local backend and JetBrains Client with the plugin in
both, as in remote development. They listen on port 5990, so disconnect any Toolbox remote session first.

Source: https://github.com/hephbuild/heph-jetbrains
