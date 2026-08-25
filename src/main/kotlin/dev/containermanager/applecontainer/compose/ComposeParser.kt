package dev.containermanager.applecontainer.compose

import org.yaml.snakeyaml.Yaml
import java.io.File

/**
 * Parses a `compose.yml` / `docker-compose.yml` into [ComposeFile]. Compose's schema allows the
 * same field to be written multiple ways (`environment` as a list of `KEY=VALUE` strings *or* a
 * map; `build` as a bare string *or* an object; ports as `"8080:80"` strings) so every accessor
 * below normalizes rather than assuming one shape.
 */
object ComposeParser {

    fun parse(file: File): ComposeFile {
        val root = Yaml().load<Map<String, Any?>>(file.readText()) ?: emptyMap()
        val servicesNode = root["services"] as? Map<*, *> ?: emptyMap<String, Any?>()

        val services = servicesNode.entries.mapNotNull { (key, value) ->
            val name = key as? String ?: return@mapNotNull null
            val serviceMap = value as? Map<*, *> ?: emptyMap<String, Any?>()
            name to parseService(name, serviceMap, file.parentFile)
        }.toMap()

        return ComposeFile(services)
    }

    private fun parseService(name: String, node: Map<*, *>, composeDir: File): ComposeService {
        return ComposeService(
            name = name,
            image = node["image"] as? String,
            build = parseBuild(node["build"], composeDir),
            containerName = node["container_name"] as? String,
            command = parseStringList(node["command"]),
            environment = parseEnvironment(node["environment"]),
            ports = parseStringList(node["ports"]),
            volumes = parseStringList(node["volumes"]),
            dependsOn = parseDependsOn(node["depends_on"]),
            networks = parseStringList(node["networks"]),
        )
    }

    private fun parseBuild(node: Any?, composeDir: File): ComposeBuild? = when (node) {
        is String -> ComposeBuild(
            context = resolvePath(composeDir, node),
            dockerfile = null,
            args = emptyList(),
            target = null,
        )

        is Map<*, *> -> ComposeBuild(
            context = resolvePath(composeDir, node["context"] as? String ?: "."),
            dockerfile = node["dockerfile"] as? String,
            args = parseEnvironment(node["args"]),
            target = node["target"] as? String,
        )

        else -> null
    }

    /** `environment` can be `["KEY=VALUE", ...]` or `{KEY: VALUE, ...}`. */
    private fun parseEnvironment(node: Any?): List<String> = when (node) {
        is List<*> -> node.mapNotNull { it?.toString() }
        is Map<*, *> -> node.entries.map { (k, v) -> "$k=${v ?: ""}" }
        else -> emptyList()
    }

    /** `depends_on` can be `["svc1", "svc2"]` or `{svc1: {condition: ...}}`. */
    private fun parseDependsOn(node: Any?): List<String> = when (node) {
        is List<*> -> node.mapNotNull { it?.toString() }
        is Map<*, *> -> node.keys.mapNotNull { it?.toString() }
        else -> emptyList()
    }

    private fun parseStringList(node: Any?): List<String> = when (node) {
        is List<*> -> node.mapNotNull { it?.toString() }
        is String -> listOf(node)
        else -> emptyList()
    }

    private fun resolvePath(baseDir: File, path: String): String =
        File(path).let { if (it.isAbsolute) it.path else File(baseDir, path).path }
}