# Apple Container Manager

Manage [Apple's `container` runtime](https://github.com/apple/container) from inside IntelliJ IDEA — without leaving the IDE or switching to a generic Docker UI.

Apple Container Manager is built around the `container` CLI's own vocabulary. It uses IntelliJ Platform concepts you already know: Tool Windows, Run Configurations, the Run console, Search Everywhere, and IDE notifications.

> **Platform note:** Requires macOS on Apple Silicon with Apple's `container` CLI installed. Network commands need macOS 26+.

## Why this plugin

- **Native IDE workflow** — containers behave like any other run target: logs, exec sessions, builds, and pulls stream into the Run tool window with Stop, ANSI coloring, and search-in-console.
- **Purpose-built for Apple Container** — not a Docker UI with a different backend; command names and workflows match `container` directly.
- **Full resource coverage** — manage containers, images, volumes, networks, and system services from one tool window.
- **Run Configurations** — launch containers with a dedicated run configuration editor (image, env vars, resources, ports, and more).
- **Search Everywhere** — jump to a container or image from anywhere in the IDE.

## Tool window

Open **View → Tool Windows → Apple Container** (bottom dock).

Five tabs organize day-to-day work:

| Tab | What you can do |
|---|---|
| **Containers** | Run, start, stop, kill, delete, view logs, exec shell, live stats, inspect |
| **Images** | Pull, push, build, run, tag, delete, prune, inspect |
| **Volumes** | Create, inspect, delete, prune |
| **Networks** | Create, inspect, delete, prune |
| **System** | Start/stop services, version info, disk usage, builder lifecycle |

Each tab includes a toolbar for common actions and a searchable table.

## Run configurations

Create a **Container Run Configuration** to launch `container run` like any other IDE run target:

1. **Run → Edit Configurations → + → Apple Container Run**
2. Set the image, container name, arguments, environment variables, and resource limits
3. Click **Run** — output streams to the Run tool window

**Dockerfile gutter icon:** open a `Dockerfile` and use the gutter run marker to build the image from that file.

## Search Everywhere

Press **Search Everywhere** (double ⇧) and type a container or image name to jump straight to it in the Apple Container tool window.

## Requirements

| Requirement | Details |
|---|---|
| **Operating system** | macOS 15+ (macOS 26+ for `container network` commands) |
| **Hardware** | Apple Silicon |
| **CLI** | [Apple `container` CLI](https://github.com/apple/container) installed and on `PATH` |
| **IDE** | IntelliJ IDEA 2024.2+ (Community or Ultimate), or any IntelliJ Platform IDE that supports the plugin |

## Getting started

1. Install Apple's `container` CLI and confirm it works in Terminal:
   ```bash
   container system version
   ```
2. Install **Apple Container Manager** from JetBrains Marketplace.
3. Open **Settings → Tools → Apple Container Manager** to set the CLI path if it is not auto-detected.
4. Open the **Apple Container** tool window and click **Refresh**.
5. (Optional) Create a Run Configuration for a container you run often.

## Settings

**Settings → Tools → Apple Container Manager**

- **CLI path** — path to the `container` binary (auto-detected when possible)
- **Poll interval** — how often the tool window refreshes runtime state
- **Confirmations** — toggle destructive-action prompts

## Keyboard & actions

Context menus and toolbars expose the same actions as the `container` CLI. Long-running operations (logs follow, exec, build, pull, push, run) open in the **Run** tool window so you can stop them with the standard Stop button.

## Links

- [Apple `container` project](https://github.com/apple/container)
- [Report an issue]([https://github.com/apple/container/issues](https://github.com/EdwardLTC/AppleContainerManager/issues))
- [IntelliJ Platform Plugin SDK](https://plugins.jetbrains.com/docs/intellij/welcome.html)

## License

Open source — add your license URL in the Marketplace **General Information** section when you publish.
