package dev.containermanager.applecontainer.compose

import dev.containermanager.applecontainer.cli.AppleContainerCli
import dev.containermanager.applecontainer.cli.model.BuildSpec
import dev.containermanager.applecontainer.cli.model.RunSpec
import java.io.File

/**
 * Drives `container build` + `container run` for each service in a [ComposeFile], in
 * dependency order. This exists because Apple's `container` CLI has no native `compose`
 * command — Compose files are a convenience layer this plugin interprets itself, not
 * something the CLI understands.
 *
 * Only the fields modeled in [ComposeService] are honored; anything Compose supports that has
 * no equivalent in `container run`/`container build` (healthchecks, `deploy`, profiles,
 * secrets, `extends`, ...) Is silently ignored rather than guessed at.
 */
class ComposeOrchestrator(private val cli: AppleContainerCli, private val projectSlug: String) {

    /** Builds (if needed) and starts every service, in dependency order. Emits progress lines via [onLine]. */
    suspend fun up(compose: ComposeFile, onLine: (String) -> Unit) {
        for (name in topologicalOrder(compose.services)) {
            val service = compose.services.getValue(name)
            val imageRef = resolveImageRef(name, service)

            if (service.build != null) {
                onLine("\n==> Building '$name' → $imageRef")
                val spec = BuildSpec(
                    contextDir = service.build.context,
                    dockerfile = service.build.dockerfile?.let { File(service.build.context, it).path },
                    tags = listOf(imageRef),
                    buildArgs = service.build.args,
                    target = service.build.target,
                )
                val exitCode = cli.images.buildAndAwait(spec) { line -> onLine(line.trimEnd('\n')) }
                if (exitCode != 0) {
                    throw ComposeException("Build failed for service '$name' (exit code $exitCode)")
                }
            } else if (service.image == null) {
                throw ComposeException("Service '$name' has neither 'image' nor 'build' — nothing to run")
            }

            onLine("\n==> Starting '$name'")
            val runSpec = RunSpec(
                image = imageRef,
                name = service.containerName ?: containerNameFor(name),
                arguments = service.command,
                env = service.environment,
                ports = service.ports,
                volumes = service.volumes,
                networks = service.networks,
                labels = listOf(
                    "com.apple.container.compose.project=$projectSlug",
                    "com.apple.container.compose.service=$name",
                ),
            )
            val id = cli.containers.runDetachedAndAwait(runSpec)
            onLine("    started ${containerNameFor(name)} ($id)")
        }
    }

    /** Stops and removes every container this orchestrator started for [compose], regardless of order. */
    suspend fun down(compose: ComposeFile, onLine: (String) -> Unit) {
        val names = compose.services.keys.map { serviceName ->
            compose.services.getValue(serviceName).containerName ?: containerNameFor(serviceName)
        }
        onLine("==> Stopping ${names.joinToString(", ")}")
        runCatching { cli.containers.stop(names) }
        runCatching { cli.containers.delete(names, force = true) }
        onLine("Done.")
    }

    private fun resolveImageRef(serviceName: String, service: ComposeService): String =
        service.image ?: "$projectSlug-$serviceName:latest"

    private fun containerNameFor(serviceName: String): String = "$projectSlug-$serviceName"

    /** Kahn-free DFS topological sort; throws on a dependency cycle rather than looping forever. */
    private fun topologicalOrder(services: Map<String, ComposeService>): List<String> {
        val visited = mutableSetOf<String>()
        val inProgress = mutableSetOf<String>()
        val order = mutableListOf<String>()

        fun visit(name: String) {
            if (name in visited) return
            if (name in inProgress) throw ComposeException("Circular 'depends_on' involving '$name'")
            val service = services[name] ?: return // dependency isn't defined as a service; ignore
            inProgress += name
            service.dependsOn.forEach(::visit)
            inProgress -= name
            visited += name
            order += name
        }

        services.keys.forEach(::visit)
        return order
    }
}