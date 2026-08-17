package dev.containermanager.applecontainer.cli

/**
 * Single entry point for talking to Apple's `container` CLI, organized to mirror the CLI's own
 * command groups (`container <group> <verb>`). Each group is a thin, independently testable
 * class; this class just wires them to a shared [CliExecutor].
 *
 * ## Extensibility
 * This plugin is intentionally scoped to Apple's `container` runtime (per design goals it is
 * *not* a generic Docker UI). That said, the split between "typed domain layer"
 * ([dev.containermanager.applecontainer.cli.model]) and "CLI transport"
 * (this file + [CliExecutor]) means a second backend (say, a hypothetical future daemon-based
 * API instead of CLI shelling) could be introduced later by providing an alternate
 * implementation of the same command-family interfaces without touching services, actions, or
 * UI code, which all depend only on the domain models.
 */
class AppleContainerCli(binaryPathProvider: () -> String?) {

    private val executor = CliExecutor(binaryPathProvider)

    val containers = ContainerCommands(executor)
    val images = ImageCommands(executor)
    val volumes = VolumeCommands(executor)
    val networks = NetworkCommands(executor)
    val registries = RegistryCommands(executor)
    val builder = BuilderCommands(executor)
    val system = SystemCommands(executor)
    val machine = MachineCommands(executor)
    val k8s = K8sCommands(executor)

    /** Cheap availability probe used on startup and before enabling actions. */
    suspend fun isCliAvailable(): Boolean =
        runCatching { executor.exec(listOf("--version")).isSuccess }.getOrDefault(false)

    suspend fun isDaemonRunning(): Boolean =
        runCatching { system.status().running }.getOrDefault(false)
}
