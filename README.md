# Apple Container Manager IntelliJ IDEA Plugin

A native IntelliJ IDEA plugin for managing [Apple's `container`](https://github.com/apple/container)
runtime, built specifically around the `container` CLI's own vocabulary rather than as a
Docker-UI-with-a-different-backend. It integrates with the IntelliJ Platform's own concepts \u2014
Tool Windows, Services, Actions, Run Configurations, Notifications, Search Everywhere, and the
Run console instead of reinventing them.

## Requirements

- macOS 15+ (macOS 26+ for `container network` commands), Apple Silicon
- [Apple's `container` CLI](https://github.com/apple/container) installed
- IntelliJ IDEA 2024.2+ (Community or Ultimate) to run the plugin
- JDK 17 and Gradle 8.x to build it

> This archive does not include the Gradle wrapper jar (no network access was available to fetch
> it while generating the project). Before building, run `gradle wrapper --gradle-version 8.7`
> once with a locally installed Gradle, or just invoke `gradle <task>` directly instead of
> `./gradlew <task>` below.

## Building

```bash
./gradlew buildPlugin
```

The packaged plugin zip is written to `build/distributions/`. Install it via
**Settings Plugins Install Plugin from Disk**, or run
`./gradlew runIde` to launch a sandboxed IDE instance with the plugin loaded.

> **Note:** this sandbox environment has no network access to JetBrains' plugin repository, so
> the project could not be compiled or verified end-to-end here. The code has been written and
> reviewed carefully against the IntelliJ Platform SDK APIs, but treat the first local build as
> the actual compilation check and expect to fix minor API-surface mismatches (IntelliJ Platform
> APIs do shift between minor versions).

## Architecture

```
cli/                    Transport layer: never touches Swing or the platform's execution UI
  CliLocator            Resolves the `container` binary path
  CliExecutor           Coroutine process runner (capturing + streaming), never blocks the EDT
  ArgBuilders           Typed spec -> CLI argument list (shared by Run Configs and quick actions)
  ContainerCommands      
  ImageCommands          One class per CLI command group, mirroring `container <group> <verb>`
  ResourceCommands       (volume/network/registry/builder)
  SystemCommands         (system/machine/k8s)
  AppleContainerCli      Facade wiring the above to one CliExecutor the extensibility seam
  model/                 Domain types (ContainerInfo, ImageInfo, RunSpec, BuildSpec, ...)
  parse/JsonMapper       Lenient container-JSON -> domain model mapping (see caveat below)

services/
  ContainerRuntimeService   Project service: owns the CLI facade, polls it, publishes
                            RuntimeSnapshot via StateFlow. UI only ever reads this flow.
  PluginScopeService        Coroutine launcher used by actions
  AppleContainerSettingsState   Persistent CLI path / poll interval / confirmation prefs

toolwindow/              Swing UI: tabs (Containers/Images/Volumes/Networks/System), each backed
                          by a ColumnInfo/ListTableModel table, collecting the snapshot flow on
                          an EDT-bound coroutine scope tied to the tool window's disposal.

actions/                 AnAction implementations per resource type; BaseCliAction centralizes
                          confirm -> run off-EDT -> notify -> refresh.

run/                     RunConfigurationType/Factory/Editor/State so `container run` is a first-
                          class Run Configuration, plus a Dockerfile gutter icon
                          (DockerfileRunLineMarkerContributor) for "build this file" affordance.

search/                  Search Everywhere contributor for jumping to a container or image.
```

### Design choices worth calling out

- **Everything streaming reuses IntelliJ's own Run console** (`RunContentExecutor` for quick
  actions, `TextConsoleBuilder` + `DefaultExecutionResult` for the Run Configuration) instead of
  a bespoke output widget. This gives Stop buttons, ANSI handling, and search-in-console for
  free and keeps `logs -f`, `exec`, `build`, `pull`, `push`, and `run` all feeling identical to
  any other IDE run target.
- **No blocking on the EDT.** `CliExecutor.exec` suspends on `Dispatchers.IO`; the only EDT work
  is Swing mutation, done via `Dispatchers.EDT` (the platform's own dispatcher) or
  `invokeLater`. `ContainerRuntimeService` polls on a background coroutine and publishes a
  `StateFlow`; panels collect it on an EDT-scoped `CoroutineScope` tied to disposal.
- **Extensibility seam:** the split between the domain model (`cli/model`) and the CLI transport
  (`CliExecutor` + the `*Commands` classes) means everything above the CLI layer \u2014 services,
  actions, tool window, run configurations \u2014 depends only on typed domain objects, never on
  CLI argument strings. A second backend could implement the same command-family shape without
  touching UI code. This plugin intentionally does not add a second backend today, per the
  project's Apple-`container`-first design goal.

## Known limitation: JSON schema is inferred, not documented

Apple's `container` CLI documents an exact JSON shape only for `system version`
(`appName`/`version`/`buildType`/`commit`). The reference for `list`/`image list`/`volume list`/
`network list --format json` does not publish field names. `cli/parse/JsonMapper.kt` uses field
names consistent with the CLI's own internal vocabulary (`configuration`, `initProcess`,
`resources`, `platform`, `networks[].network`, ...) inferred from the tool's structure, and every
accessor is written to degrade gracefully (`ignoreUnknownKeys = true`, nullable chains, `runCatching`
per row) rather than throw on a mismatch. **If real-world output differs, only
`JsonMapper.kt` needs updating** \u2014 no other file depends on the raw JSON shape.

## What's implemented vs. exposed-but-unstyled

Full CLI coverage exists in the `cli/` layer for every command in the provided reference,
including `container system dns`, `container system kernel`, `container machine`, and
`container k8s`. The Tool Window UI focuses on the workflows developers touch constantly
(containers, images, volumes, networks, system/builder lifecycle); `machine` and `k8s` are wired
through the CLI facade (`cli.machine.*`, `cli.k8s.*`) and ready for a dedicated tab but don't yet
have one, since single-node k8s and VM-image machines are comparatively rare operations. Adding a
tab for either is a small, mechanical extension of the existing `toolwindow/tabs` pattern.

## Extending

- **New CLI command:** add a method to the relevant `*Commands` class in `cli/`; nothing else
  needs to change unless you're also exposing it in the UI.
- **New tool window tab:** copy the shape of `VolumesPanel` + `VolumeTableModelFactory`, add a
  `RuntimeSnapshot` field if the data should be polled, and register a toolbar action group in
  `plugin.xml`.
- **New action:** extend `BaseCliAction` for anything that's "confirm, run off-EDT, refresh"; use
  a plain `AnAction` for anything that opens a console/dialog instead (see the `container/`
  streaming actions for the pattern).
