package dev.containermanager.applecontainer.compose

import dev.containermanager.applecontainer.cli.AppleContainerCli
import dev.containermanager.applecontainer.cli.model.BuildSpec
import dev.containermanager.applecontainer.cli.model.ContainerStatus
import dev.containermanager.applecontainer.cli.model.RunSpec
import java.io.File
import java.security.MessageDigest

/**
 * Compose-up implementation for Apple's `container` runtime.
 *
 * It follows the Opossum model: services run in dependency order, on project-scoped networks,
 * with project labels and a stable configuration hash so repeat `up` calls are safe.
 */
class ComposeOrchestrator(private val cli: AppleContainerCli) {

    suspend fun up(compose: ComposeFile, onLine: (String) -> Unit) {
        val order = topologicalOrder(compose.services)
        val dnsDomain = availableDnsDomain()
        if (dnsDomain == null) {
            onLine("warning: service-name DNS is unavailable. Create it once with: sudo container system dns create $DNS_DOMAIN")
        }
        compose.warnings.forEach { onLine("warning: $it") }

        val createdNetworks = mutableListOf<String>()
        val startedContainers = mutableListOf<String>()
        try {
            ensureNetworks(compose, order, createdNetworks, onLine)
            ensureVolumes(compose, order, onLine)
            val containers = cli.containers.list(all = true)

            for (name in order) {
                val service = compose.services.getValue(name)
                val image = imageReference(compose, service)
                if (service.build != null) buildService(name, image, service.build, onLine)

                val spec = runSpec(compose, service, image, dnsDomain)
                val expectedHash = configHash(spec)
                val existing = containers.firstOrNull { it.displayName == spec.name }
                if (existing != null && existing.status == ContainerStatus.RUNNING && existing.labels[CONFIG_HASH_LABEL] == expectedHash) {
                    onLine("==> $name is up to date")
                    continue
                }

                if (existing != null) {
                    onLine("==> Recreating '$name'")
                    cli.containers.delete(listOf(existing.id), force = true)
                } else {
                    onLine("==> Starting '$name'")
                }
                val id = cli.containers.runDetachedAndAwait(
                    spec.copy(labels = spec.labels + "$CONFIG_HASH_LABEL=$expectedHash"),
                )
                startedContainers += spec.name.orEmpty()
                onLine("    started ${spec.name} (${id.ifBlank { "running" }})")
            }
        } catch (error: Throwable) {
            onLine("Compose up failed; rolling back containers started by this run…")
            startedContainers.asReversed()
                .forEach { id -> runCatching { cli.containers.delete(listOf(id), force = true) } }
            createdNetworks.asReversed().forEach { name -> runCatching { cli.networks.delete(listOf(name)) } }
            throw error as? ComposeException
                ?: ComposeException(
                    error.message ?: "Compose up failed",
                    error
                )
        }
    }

    /** Stops/removes only containers labeled as belonging to this Compose project. */
    suspend fun down(compose: ComposeFile, onLine: (String) -> Unit) {
        val containers = cli.containers.list(all = true).filter {
            it.labels[PROJECT_LABEL] == compose.projectName || it.labels[LEGACY_PROJECT_LABEL] == compose.projectName
        }
        if (containers.isNotEmpty()) {
            onLine("==> Stopping ${containers.joinToString { it.displayName }}")
            runCatching { cli.containers.stop(containers.map { it.id }) }
            cli.containers.delete(containers.map { it.id }, force = true)
        }
        managedNetworks(compose, compose.services.keys).asReversed().forEach { network ->
            if (!network.external) runCatching { cli.networks.delete(listOf(network.runtimeName)) }
        }
        onLine("Done.")
    }

    private suspend fun buildService(name: String, image: String, build: ComposeBuild, onLine: (String) -> Unit) {
        onLine("==> Building '$name' → $image")
        val spec = BuildSpec(
            contextDir = build.context,
            dockerfile = build.dockerfile?.let { File(build.context, it).path },
            tags = listOf(image),
            buildArgs = build.args,
            target = build.target,
        )
        val exitCode = cli.images.buildAndAwait(spec) { line -> onLine(line.trimEnd()) }
        if (exitCode != 0) throw ComposeException("Build failed for service '$name' (exit code $exitCode)")
    }

    private suspend fun ensureNetworks(
        project: ComposeFile,
        services: List<String>,
        created: MutableList<String>,
        onLine: (String) -> Unit,
    ) {
        val existing = cli.networks.list().map { it.name }.toSet()
        for ((runtimeName, external, internal) in managedNetworks(project, services)) {
            if (external) {
                if (runtimeName !in existing) throw ComposeException(
                    "Network '$runtimeName' is external but does not exist. Create it first or remove external: true.",
                )
            } else if (runtimeName !in existing) {
                onLine("==> Creating network '$runtimeName'")
                cli.networks.create(
                    name = runtimeName,
                    internal = internal,
                    labels = listOf("$PROJECT_LABEL=${project.projectName}"),
                )
                created += runtimeName
            }
        }
    }

    private suspend fun ensureVolumes(project: ComposeFile, services: List<String>, onLine: (String) -> Unit) {
        val existing = cli.volumes.list().map { it.name }.toSet()
        val names = services.flatMap { service -> project.services.getValue(service).volumes }
            .mapNotNull { namedVolumeSource(it) }
            .distinct()
        for (logicalName in names) {
            val volume = project.volumes[logicalName] ?: throw ComposeException(
                "Service references named volume '$logicalName', but it is not declared at top level.",
            )
            val runtimeName = resolveVolumeName(project, volume)
            if (!volume.external && runtimeName !in existing) {
                onLine("==> Creating volume '$runtimeName'")
                cli.volumes.create(runtimeName, labels = listOf("$PROJECT_LABEL=${project.projectName}"))
            } else if (volume.external && runtimeName !in existing) {
                throw ComposeException("Volume '$runtimeName' is external but does not exist.")
            }
        }
    }

    private fun runSpec(project: ComposeFile, service: ComposeService, image: String, dnsDomain: String?): RunSpec {
        val dnsSearch = dnsDomain?.let { "${project.projectName}.$it" }
        val containerName =
            if (dnsSearch == null) "${project.projectName}-${service.name}" else "${service.name}.$dnsSearch"
        return RunSpec(
            image = image,
            name = containerName,
            arguments = service.command,
            entrypoint = service.entrypoint,
            env = resolveEnvironment(service),
            ports = service.ports,
            volumes = service.volumes.map { resolveMount(project, it) },
            networks = serviceNetworks(project, service),
            platform = service.platform,
            workdir = service.workingDir,
            user = service.user,
            memory = service.memory,
            cpus = service.cpus,
            dnsDomain = dnsDomain,
            dnsSearch = dnsSearch,
            labels = service.labels + listOf(
                "$PROJECT_LABEL=${project.projectName}",
                "$LEGACY_PROJECT_LABEL=${project.projectName}",
                "$SERVICE_LABEL=${service.name}",
            ),
        )
    }

    private fun resolveEnvironment(service: ComposeService): List<String> {
        val fromFiles = linkedMapOf<String, String>()
        service.envFiles.forEach { path ->
            val file = File(path)
            if (!file.isFile) throw ComposeException("Service '${service.name}': env_file '$path' does not exist")
            file.readLines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith('#')) return@forEach
                val equals = trimmed.indexOf('=')
                if (equals <= 0) throw ComposeException("Service '${service.name}': invalid env_file entry '$line'")
                fromFiles[trimmed.substring(0, equals)] = trimmed.substring(equals + 1)
            }
        }
        service.environment.forEach { entry ->
            val equals = entry.indexOf('=')
            if (equals > 0) fromFiles[entry.substring(0, equals)] = entry.substring(equals + 1)
        }
        return fromFiles.map { (key, value) -> "$key=$value" }
    }

    private fun resolveMount(project: ComposeFile, rawMount: String): String {
        val parts = rawMount.split(':', limit = 3)
        if (parts.size < 2) return rawMount
        val source = parts.first()
        if (isBindPath(source)) {
            val resolved = File(source).let { if (it.isAbsolute) it.path else File(project.composeDir, source).path }
            return listOf(resolved, *parts.drop(1).toTypedArray()).joinToString(":")
        }
        val volume = project.volumes[source] ?: return rawMount
        return listOf(resolveVolumeName(project, volume), *parts.drop(1).toTypedArray()).joinToString(":")
    }

    private fun serviceNetworks(project: ComposeFile, service: ComposeService): List<String> {
        if (service.networkMode == "none") return listOf("none")
        return networksFor(project, service).map { it.runtimeName }
    }

    private fun managedNetworks(project: ComposeFile, services: Collection<String>): List<ResolvedNetwork> =
        services.flatMap { networksFor(project, project.services.getValue(it)) }
            .distinctBy { it.runtimeName }
            .sortedBy { it.runtimeName }

    private fun networksFor(project: ComposeFile, service: ComposeService): List<ResolvedNetwork> {
        if (service.networkMode == "none") return emptyList()
        val names = service.networks.ifEmpty { listOf(DEFAULT_NETWORK) }
        return names.map { name ->
            if (name == DEFAULT_NETWORK && name !in project.networks) {
                ResolvedNetwork("${project.projectName}-$DEFAULT_NETWORK", external = false, internal = false)
            } else {
                val network = project.networks[name]
                    ?: throw ComposeException("Service '${service.name}' references unknown network '$name'")
                val actualName = if (network.external) network.runtimeName
                    ?: network.name else "${project.projectName}-${network.runtimeName ?: network.name}"
                ResolvedNetwork(actualName, network.external, network.internal)
            }
        }
    }

    private suspend fun availableDnsDomain(): String? {
        val output = runCatching { cli.system.dnsList() }.getOrNull() ?: return null
        return DNS_DOMAIN.takeIf {
            output.lineSequence().any { line -> line.trim().split(Regex("\\s+")).firstOrNull() == it }
        }
    }

    private fun imageReference(project: ComposeFile, service: ComposeService): String =
        service.image ?: "${project.projectName}-${service.name}:latest"

    private fun resolveVolumeName(project: ComposeFile, volume: ComposeVolume): String =
        if (volume.external) volume.runtimeName
            ?: volume.name else "${project.projectName}-${volume.runtimeName ?: volume.name}"

    private fun namedVolumeSource(mount: String): String? {
        val source = mount.substringBefore(':', missingDelimiterValue = "")
        return source.takeIf { it.isNotEmpty() && !isBindPath(it) }
    }

    private fun isBindPath(source: String): Boolean =
        source.startsWith("/") || source.startsWith("./") || source.startsWith("../") || source.startsWith('~')

    private fun topologicalOrder(services: Map<String, ComposeService>): List<String> {
        val visited = mutableSetOf<String>()
        val inProgress = mutableSetOf<String>()
        val order = mutableListOf<String>()
        fun visit(name: String) {
            if (name in visited) return
            if (name in inProgress) throw ComposeException("Circular 'depends_on' involving '$name'")
            val service = services[name] ?: throw ComposeException("Service '$name' depends on an undefined service")
            inProgress += name
            service.dependsOn.sorted().forEach(::visit)
            inProgress -= name
            visited += name
            order += name
        }
        services.keys.sorted().forEach(::visit)
        return order
    }

    private fun configHash(spec: RunSpec): String = MessageDigest.getInstance("SHA-256")
        .digest(
            listOf(
                spec.image,
                spec.name,
                spec.arguments.joinToString("\u0000"),
                spec.entrypoint.orEmpty(),
                spec.env.sorted().joinToString("\u0000"),
                spec.ports.sorted().joinToString("\u0000"),
                spec.volumes.sorted().joinToString("\u0000"),
                spec.networks.joinToString("\u0000"),
                spec.platform.orEmpty(),
                spec.workdir.orEmpty(),
                spec.user.orEmpty(),
                spec.memory.orEmpty(),
                spec.cpus.orEmpty(),
            ).joinToString("\u0001").toByteArray(),
        ).joinToString("") { "%02x".format(it) }

    private data class ResolvedNetwork(val runtimeName: String, val external: Boolean, val internal: Boolean)

    private companion object {
        const val DEFAULT_NETWORK = "default"
        const val DNS_DOMAIN = "applecontainer"
        const val PROJECT_LABEL = "dev.containermanager.applecontainer.compose.project"
        const val LEGACY_PROJECT_LABEL = "com.apple.container.compose.project"
        const val SERVICE_LABEL = "dev.containermanager.applecontainer.compose.service"
        const val CONFIG_HASH_LABEL = "dev.containermanager.applecontainer.compose.config-hash"
    }
}
