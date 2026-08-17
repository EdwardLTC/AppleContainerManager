# Apple Container Manager — IntelliJ Plugin (Phase 1)

Native IntelliJ IDEA tool window for Apple's `container` CLI/runtime.

## ⚠️ Environment disclaimer — read before trusting the CLI details

This was built in a Linux sandbox with no macOS host and no network access to
Maven Central / the Gradle Plugin Portal / JetBrains' IntelliJ artifact repos
(the sandbox's egress allowlist only covers npm/pypi/crates/GitHub). Concretely,
that means:

- **The Gradle project has never actually been built or run here.** No
  `./gradlew build`, no `runIde`, no compiler check. Everything below is
  correct-by-careful-reading of the IntelliJ Platform SDK and Kotlin, not
  correct-by-compilation. Run `./gradlew build` yourself as the very first
  step (see below) before relying on any of it.
- **The exact `container` CLI subcommands/flags/JSON schema are unverified.**
  I don't have access to a real `container` binary to inspect `container ls
  --all --format json`, `container images ls`, or the machine/VM status
  command. Section 26 of the brief asked me to inspect the live CLI before
  writing parsers — I couldn't, so instead:
   - Every raw command lives in **one file**,
     [`AppleContainerCommandBuilder.kt`](src/main/kotlin/com/acm/plugin/cli/AppleContainerCommandBuilder.kt),
     with `TODO(verify)` comments on the calls I'm least sure about (the
     machine-status subcommand name, `images` vs `image`, `unpause` vs
     `resume`).
   - Every parser (`ContainerParser`, `ImageParser`, `MachineParser`) is
     defensive: it tries a handful of plausible key spellings per field and
     degrades to `null`/`UNKNOWN` instead of throwing on an unrecognized shape.
   - Parsers are unit-tested against **assumed** fixture JSON — the tests
     document the current assumption, not verified ground truth. The very
     first thing to do on a real Mac is run `container ls --all --format json`
     by hand, diff it against the fixtures in
     `src/test/kotlin/com/acm/plugin/cli/parser/`, and fix whichever of
     schema/builder/parser is wrong.
- The Gradle wrapper jar isn't included (couldn't fetch it). Run
  `gradle wrapper --gradle-version 8.9` once you have a normal internet
  connection, or open the project in IntelliJ and let it generate one.

None of this is hidden in the code — every place I guessed is flagged with
`TODO(verify)` or a doc comment saying so.

## What Phase 1 does

- Registers an **"Apple Container" Tool Window** (bottom, Services-style tree).
- On open, detects whether the `container` CLI is on `PATH`.
- If present, lists **Machines**, **Containers**, and **Images** in a tree,
  refreshed manually via the toolbar Refresh action (also in Find Action /
  Search Everywhere as "Apple Container: Refresh").
- Status is colored/iconified per row (running/stopped/paused/error/unknown).
- Handles the documented empty/error states: CLI not found, machine not
  running, load failure (with retry), and loading.
- Read-only: no start/stop/logs/terminal/run yet — see "Not in Phase 1" below.

## Architecture

```
UI (Tool Window / Tree)
   ↓ observes StateFlow
AppleContainerService (project service)
   ↓ calls
ContainerConnection (interface)
   ↓ implemented by
AppleContainerConnection
   ↓ delegates to
AppleContainerClient  (execute + parse)
   ↓ uses
AppleContainerCommandBuilder (builds argv)   +   cli.parser.* (JSON → models)
   ↓
ProcessExecutor (coroutine-based process execution, never touches Swing)
   ↓
`container` CLI
```

Key decisions:

- **`ContainerConnection` is an interface** so a future
  `RemoteAppleContainerConnection` (e.g. SSH'd into another Mac) can be added
  without touching `AppleContainerService` or any UI code — the service and
  UI only ever depend on `ContainerConnection`.
- **All CLI argv construction lives in `AppleContainerCommandBuilder`.**
  Nothing else in the plugin builds a `container ...` string.
- **All process spawning goes through `ProcessExecutor`.** UI code cannot
  call `Runtime.exec` or a CLI client directly — everything routes through
  `AppleContainerService`, which owns a `CoroutineScope` and a `StateFlow<AppleContainerState>`.
- **Parsing is separated from execution** (`cli/parser/*`) specifically so it
  can be unit tested with fixture strings, no process spawning or IDE
  fixture needed. See `src/test/kotlin`.
- **State is a single `StateFlow`** (`AppleContainerState`: machines,
  containers, images, loading, error, lastUpdated) that the tree panel
  renders from, rather than the UI mutating itself in multiple places.
  `AppleContainerService.refresh()` cancels any in-flight refresh before
  starting a new one, so rapid clicks don't pile up overlapping CLI calls.
- **Nothing blocks the EDT.** All CLI calls are `suspend` functions run on
  `Dispatchers.Default`/IO via `ProcessExecutor`; the tree panel collects the
  state flow and hops to `Dispatchers.EDT` only to paint.

### Project layout

```
src/main/kotlin/com/acm/plugin/
  cli/            AppleContainerCommandBuilder, AppleContainerClient, ProcessExecutor
  cli/parser/      ContainerParser, ImageParser, MachineParser
  connection/     ContainerConnection (interface), AppleContainerConnection
  model/          AppContainer, ContainerImage, Machine, ContainerStatus, MachineStatus
  service/        AppleContainerService (project), CliDetectionService (application)
  state/          AppleContainerState, CliAvailability
  ui/             AppleContainerToolWindowFactory, AppleContainerPanel,
                  AppleContainerTreePanel, EmptyStatePanel
  action/         RefreshAction
  notification/   AppleContainerNotifier
src/test/kotlin/com/acm/plugin/cli/           command + parser unit tests
```

`runconfig/` and `util/` are in the brief's target layout but intentionally
empty in Phase 1 — nothing to put there yet (Run Configurations are Phase 4).

## Explicitly NOT in Phase 1

Per the phased plan in the brief: start/stop/restart/remove actions, pull
image, context menus, notifications on actions, container details view, log
viewer, in-IDE terminal, Run Container dialog, Run Configuration, Services
tool window integration, volumes, networks, resource monitoring, and
project-aware container grouping. The architecture (connection interface,
command builder, StateFlow) is deliberately shaped so all of those are
additive rather than requiring rework — e.g. adding "Stop" just means a new
action calling `AppleContainerService`/`connection.stopContainer(id)` plus a
context-menu entry, not a new execution pathway.

## Before you build

1. `gradle wrapper --gradle-version 8.9` (or open in IntelliJ to regenerate
   `gradlew`/the wrapper jar).
2. Open in IntelliJ IDEA (Community or Ultimate) with the **Plugin DevKit**
   and **Kotlin** plugins enabled.
3. Confirm the target IDE version in `build.gradle.kts`
   (`intellijIdeaCommunity("2024.2")`) matches what you actually want to
   target; bump it if needed.
4. `./gradlew test` — should pass without touching a real IDE or CLI.
5. `./gradlew runIde` — launches a sandbox IDE with the plugin installed.
6. On a Mac with `container` installed, run `container ls --all --format
   json` / `container images ls --format json` / whatever the real
   machine-status command turns out to be, and reconcile the output against
   `src/test/kotlin/com/acm/plugin/cli/parser/*Test.kt` and the `TODO(verify)`
   markers in `AppleContainerCommandBuilder.kt`.

## Recommended next step

Phase 2 (start/stop/restart/remove, pull image, context menus, action
notifications) — but only after step 6 above confirms the CLI assumptions,
since several Phase 2 actions (pause/resume in particular) are exactly the
ones flagged as unverified.
