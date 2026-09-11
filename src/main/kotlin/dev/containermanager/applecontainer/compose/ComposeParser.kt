package dev.containermanager.applecontainer.compose

import org.yaml.snakeyaml.Yaml
import java.io.File

/**
 * Parses the Compose fields that have a direct Apple Container equivalent. Unsupported fields
 * are retained as warnings so an `up` never silently claims to have applied them.
 */
object ComposeParser {

    fun parse(file: File): ComposeFile {
        val root = Yaml().load<Map<String, Any?>>(interpolate(file.readText(), loadDotEnv(file.parentFile))) ?: emptyMap()
        val composeDir = file.parentFile
        val projectName = slug(root["name"]?.toString().takeUnless { it.isNullOrBlank() } ?: composeDir.name)
        val warnings = mutableListOf<String>()
        val servicesNode = root["services"] as? Map<*, *> ?: throw ComposeException("Compose file has no 'services' mapping")
        val services = servicesNode.entries.associate { (key, value) ->
            val name = key?.toString()?.takeIf { it.isNotBlank() } ?: throw ComposeException("Compose service has no name")
            val node = value as? Map<*, *> ?: throw ComposeException("Service '$name' must be a mapping")
            name to parseService(name, node, composeDir, warnings)
        }
        if (services.isEmpty()) throw ComposeException("Compose file has no services")

        return ComposeFile(
            projectName = projectName,
            composeDir = composeDir,
            services = services,
            volumes = parseVolumes(root["volumes"], warnings),
            networks = parseNetworks(root["networks"], warnings),
            warnings = warnings,
        )
    }

    private fun parseService(
        name: String,
        node: Map<*, *>,
        composeDir: File,
        warnings: MutableList<String>,
    ): ComposeService {
        val supported = setOf(
            "image", "build", "command", "entrypoint", "environment", "env_file", "ports", "volumes",
            "depends_on", "networks", "platform", "working_dir", "user", "mem_limit", "cpus", "deploy",
            "labels", "network_mode",
        )
        node.keys.mapNotNull { it?.toString() }
            .filter { it !in supported && !it.startsWith("x-") }
            .forEach { warnings += "Service '$name': '$it' is not supported and will be ignored" }

        return ComposeService(
            name = name,
            image = node["image"]?.toString(),
            build = parseBuild(node["build"], composeDir),
            command = parseCommand(node["command"]),
            entrypoint = parseEntrypoint(node["entrypoint"]),
            environment = parseKeyValues(node["environment"]),
            envFiles = parseEnvFiles(node["env_file"], composeDir),
            ports = parsePorts(node["ports"]),
            volumes = parseVolumeMounts(node["volumes"]),
            dependsOn = parseDependsOn(node["depends_on"]),
            networks = parseNetworkNames(node["networks"]),
            platform = node["platform"]?.toString(),
            workingDir = node["working_dir"]?.toString(),
            user = node["user"]?.toString(),
            memory = node["mem_limit"]?.toString() ?: parseDeployMemory(node["deploy"]),
            cpus = node["cpus"]?.toString() ?: parseDeployCpus(node["deploy"]),
            labels = parseKeyValues(node["labels"]),
            networkMode = node["network_mode"]?.toString(),
        )
    }

    private fun parseBuild(node: Any?, composeDir: File): ComposeBuild? = when (node) {
        is String -> ComposeBuild(resolvePath(composeDir, node), null, emptyList(), null)
        is Map<*, *> -> ComposeBuild(
            context = resolvePath(composeDir, node["context"]?.toString() ?: "."),
            dockerfile = node["dockerfile"]?.toString(),
            args = parseKeyValues(node["args"]),
            target = node["target"]?.toString(),
        )
        null -> null
        else -> throw ComposeException("'build' must be a string or mapping")
    }

    private fun parseCommand(node: Any?): List<String> = when (node) {
        null -> emptyList()
        is String -> listOf("/bin/sh", "-c", node)
        is List<*> -> node.mapNotNull { it?.toString() }
        else -> throw ComposeException("'command' must be a string or list")
    }

    private fun parseEntrypoint(node: Any?): String? = when (node) {
        null -> null
        is String -> node
        is List<*> -> node.joinToString(" ") { it.toString() }
        else -> throw ComposeException("'entrypoint' must be a string or list")
    }

    private fun parseKeyValues(node: Any?): List<String> = when (node) {
        null -> emptyList()
        is List<*> -> node.mapNotNull { item -> item?.toString() }
        is Map<*, *> -> node.entries.mapNotNull { (key, value) ->
            val name = key?.toString() ?: return@mapNotNull null
            "$name=${value ?: System.getenv(name).orEmpty()}"
        }
        else -> throw ComposeException("Expected a key-value map or list")
    }

    private fun parseEnvFiles(node: Any?, composeDir: File): List<String> = parseStringList(node).map { resolvePath(composeDir, it) }

    private fun parsePorts(node: Any?): List<String> = when (node) {
        null -> emptyList()
        is List<*> -> node.mapNotNull { port ->
            when (port) {
                is Map<*, *> -> port["target"]?.toString()?.let { target ->
                    val published = port["published"]?.toString()
                    val host = port["host_ip"]?.toString()
                    val protocol = port["protocol"]?.toString()?.takeUnless { it == "tcp" }.orEmpty()
                    listOfNotNull(host, published, target).joinToString(":") + protocol.takeIf { it.isNotEmpty() }?.let { "/$it" }.orEmpty()
                }
                else -> port?.toString()
            }
        }
        else -> parseStringList(node)
    }

    private fun parseVolumeMounts(node: Any?): List<String> = when (node) {
        null -> emptyList()
        is List<*> -> node.mapNotNull { mount ->
            when (mount) {
                is Map<*, *> -> {
                    val type = mount["type"]?.toString() ?: "volume"
                    val source = mount["source"]?.toString()
                    val target = mount["target"]?.toString() ?: return@mapNotNull null
                    if (type == "tmpfs") target else source?.let { "$it:$target" }
                }
                else -> mount?.toString()
            }
        }
        else -> parseStringList(node)
    }

    private fun parseDependsOn(node: Any?): List<String> = when (node) {
        is List<*> -> node.mapNotNull { it?.toString() }
        is Map<*, *> -> node.keys.mapNotNull { it?.toString() }
        null -> emptyList()
        else -> throw ComposeException("'depends_on' must be a list or mapping")
    }

    private fun parseNetworkNames(node: Any?): List<String> = when (node) {
        is List<*> -> node.mapNotNull { it?.toString() }
        is Map<*, *> -> node.keys.mapNotNull { it?.toString() }
        null -> emptyList()
        else -> throw ComposeException("'networks' must be a list or mapping")
    }

    private fun parseVolumes(node: Any?, warnings: MutableList<String>): Map<String, ComposeVolume> =
        (node as? Map<*, *>)?.entries?.associate { (key, value) ->
            val name = key.toString()
            val options = value as? Map<*, *> ?: emptyMap<String, Any?>()
            warnUnsupported("Volume '$name'", options, setOf("external", "name"), warnings)
            name to ComposeVolume(name, options["external"] == true || options["external"] == "true", options["name"]?.toString())
        }.orEmpty()

    private fun parseNetworks(node: Any?, warnings: MutableList<String>): Map<String, ComposeNetwork> =
        (node as? Map<*, *>)?.entries?.associate { (key, value) ->
            val name = key.toString()
            val options = value as? Map<*, *> ?: emptyMap<String, Any?>()
            warnUnsupported("Network '$name'", options, setOf("external", "internal", "name"), warnings)
            name to ComposeNetwork(
                name,
                options["external"] == true || options["external"] == "true",
                options["internal"] == true || options["internal"] == "true",
                options["name"]?.toString(),
            )
        }.orEmpty()

    private fun warnUnsupported(owner: String, node: Map<*, *>, supported: Set<String>, warnings: MutableList<String>) {
        node.keys.mapNotNull { it?.toString() }.filter { it !in supported && !it.startsWith("x-") }
            .forEach { warnings += "$owner: '$it' is not supported and will be ignored" }
    }

    private fun parseDeployMemory(node: Any?): String? = (((node as? Map<*, *>)?.get("resources") as? Map<*, *>)?.get("limits") as? Map<*, *>)?.get("memory")?.toString()
    private fun parseDeployCpus(node: Any?): String? = (((node as? Map<*, *>)?.get("resources") as? Map<*, *>)?.get("limits") as? Map<*, *>)?.get("cpus")?.toString()
    private fun parseStringList(node: Any?): List<String> = when (node) {
        is List<*> -> node.mapNotNull { it?.toString() }
        is String -> listOf(node)
        null -> emptyList()
        else -> throw ComposeException("Expected a string or list")
    }
    private fun resolvePath(baseDir: File, path: String): String = File(path).let { if (it.isAbsolute) it.path else File(baseDir, path).path }
    private fun slug(value: String): String = value.lowercase().replace(Regex("[^a-z0-9_-]"), "-").trim('-').ifBlank { "compose" }

    private fun loadDotEnv(composeDir: File): Map<String, String> {
        val values = System.getenv().toMutableMap()
        val file = File(composeDir, ".env")
        if (!file.isFile) return values
        file.readLines().forEachIndexed { index, line ->
            val entry = line.trim()
            if (entry.isEmpty() || entry.startsWith('#')) return@forEachIndexed
            val separator = entry.indexOf('=')
            if (separator <= 0) throw ComposeException(".env:${index + 1}: expected KEY=VALUE")
            val key = entry.substring(0, separator).removePrefix("export ").trim()
            val value = entry.substring(separator + 1).trim().removeSurrounding("\"").removeSurrounding("'")
            if (key.isEmpty()) throw ComposeException(".env:${index + 1}: empty variable name")
            values.putIfAbsent(key, value) // The process environment wins, matching docker compose.
        }
        return values
    }

    private fun interpolate(text: String, environment: Map<String, String>): String {
        val expression = Regex("\\$\\{([A-Za-z_][A-Za-z0-9_]*)(?:(:-|-|:\\?|\\?)([^}]*))?}")
        return expression.replace(text) { match ->
            val value = environment[match.groupValues[1]]
            val operator = match.groupValues[2]
            val fallback = match.groupValues[3]
            when (operator) {
                ":-" -> value?.takeIf { it.isNotEmpty() } ?: fallback
                "-" -> value ?: fallback
                ":?" -> value?.takeIf { it.isNotEmpty() } ?: throw ComposeException(fallback.ifBlank { "${match.groupValues[1]} is required" })
                "?" -> value ?: throw ComposeException(fallback.ifBlank { "${match.groupValues[1]} is required" })
                else -> value.orEmpty()
            }
        }.replace("$$", "$")
    }
}
