package dev.containermanager.applecontainer.cli.parse

import dev.containermanager.applecontainer.cli.model.*
import kotlinx.serialization.json.*

/**
 * Maps `container ... --format json` output onto this plugin's domain models.
 *
 * IMPORTANT: Apple's `container` CLI does not publish a formal JSON schema in the command
 * reference (only `system version` documents its exact shape). The field names used below
 * follow the CLI's own vocabulary (`configuration`, `status`, `initProcess`, `resources`,
 * `platform`, ...) as observed from the tool's Swift source layout, but are intentionally
 * parsed *leniently*: every accessor here tolerates missing/renamed fields and falls back to
 * sane defaults rather than throwing, and unknown keys are ignored. If a future CLI release
 * changes field names, update this file only \u2014 nothing above the CLI layer needs to change.
 */
object JsonMapper {

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    // ---- Containers -----------------------------------------------------------------

    fun parseContainerList(raw: String): List<ContainerInfo> {
        val root = safeParseArray(raw) ?: return emptyList()
        return root.mapNotNull { runCatching { mapContainer(it.jsonObject) }.getOrNull() }
    }

    private fun mapContainer(obj: JsonObject): ContainerInfo {
        val config = obj["configuration"]?.jsonObject
        val status = obj["status"]?.jsonObject

        val id = obj["id"]?.jsonPrimitive?.contentOrNull
            ?: config?.get("id")?.jsonPrimitive?.contentOrNull
            ?: "unknown"

        val image = config
            ?.get("image")
            ?.jsonObject
            ?.get("reference")
            ?.jsonPrimitive
            ?.contentOrNull
            ?: "unknown"

        val statusRaw = status
            ?.get("state")
            ?.jsonPrimitive
            ?.contentOrNull

        val resources = config
            ?.get("resources")
            ?.jsonObject

        val platform = config
            ?.get("platform")
            ?.jsonObject

        val initProcess = config
            ?.get("initProcess")
            ?.jsonObject

        val networks = status
            ?.get("networks")
            ?.jsonArray
            ?.mapNotNull {
                it.jsonObject["network"]
                    ?.jsonPrimitive
                    ?.contentOrNull
            }
            ?: emptyList()

        val labels = config
            ?.get("labels")
            ?.jsonObject
            ?.mapValues {
                it.value.jsonPrimitive.contentOrNull ?: ""
            }
            ?: emptyMap()

        val ports = config
            ?.get("publishedPorts")
            ?.jsonArray
            ?.mapNotNull { element ->
                val port = element.jsonObject

                val hostPort = port["hostPort"]
                    ?.jsonPrimitive
                    ?.intOrNull

                val containerPort = port["containerPort"]
                    ?.jsonPrimitive
                    ?.intOrNull

                if (hostPort == null || containerPort == null) {
                    null
                } else {
                    PortMapping(
                        hostIp = port["hostIp"]
                            ?.jsonPrimitive
                            ?.contentOrNull,

                        hostPort = hostPort,

                        containerPort = containerPort,

                        protocol = port["protocol"]
                            ?.jsonPrimitive
                            ?.contentOrNull
                            ?: "tcp",
                    )
                }
            }
            ?: emptyList()

        return ContainerInfo(
            id = id,
            name = labels["name"] ?: id,
            image = image,
            status = ContainerStatus.parse(statusRaw),

            createdAt = config
                ?.get("creationDate")
                ?.jsonPrimitive
                ?.contentOrNull,

            command = initProcess
                ?.get("arguments")
                ?.jsonArray
                ?.mapNotNull { it.jsonPrimitive.contentOrNull }
                ?: emptyList(),

            cpus = resources
                ?.get("cpus")
                ?.jsonPrimitive
                ?.intOrNull,

            memory = resources
                ?.get("memoryInBytes")
                ?.jsonPrimitive
                ?.longOrNull
                ?.let(::humanBytes),

            ports = ports,

            networks = networks,

            labels = labels,

            arch = platform
                ?.get("architecture")
                ?.jsonPrimitive
                ?.contentOrNull,

            os = platform
                ?.get("os")
                ?.jsonPrimitive
                ?.contentOrNull,

            platform = platform
                ?.get("architecture")
                ?.jsonPrimitive
                ?.contentOrNull,
        )
    }

    // ---- Images -----------------------------------------------------------------------

    fun parseImageList(raw: String): List<ImageInfo> {
        val root = safeParseArray(raw) ?: return emptyList()

        fun parseExposedPorts(variant: JsonObject): List<ExposedPort> {
            return variant["config"]
                ?.jsonObject
                ?.get("history")
                ?.jsonArray
                ?.mapNotNull {
                    it.jsonObject["created_by"]
                        ?.jsonPrimitive
                        ?.contentOrNull
                }
                ?.flatMap { createdBy ->
                    Regex("""EXPOSE\s+\[([^]]+)]""").findAll(createdBy).flatMap { match ->
                        match.groupValues[1]
                            .split(",")
                            .asSequence()
                            .mapNotNull { value ->
                                val parts = value.trim().split("/")

                                val port = parts
                                    .firstOrNull()
                                    ?.toIntOrNull()
                                    ?: return@mapNotNull null

                                ExposedPort(
                                    port = port,
                                    protocol = parts.getOrElse(1) { "tcp" }
                                )
                            }
                    }.toList()
                }
                ?.distinct()
                ?: emptyList()
        }

        return root.mapNotNull { el ->
            runCatching {
                val obj = el.jsonObject
                val configuration = obj["configuration"]?.jsonObject
                val variant = obj["variants"]?.jsonArray?.firstOrNull()?.jsonObject
                val platform = variant?.get("platform")?.jsonObject

                ImageInfo(
                    reference = configuration?.get("name")?.jsonPrimitive?.contentOrNull ?: "unknown",
                    id = obj["id"]?.jsonPrimitive?.contentOrNull,
                    digest = obj["digest"]?.jsonPrimitive?.contentOrNull,
                    createdAt = configuration?.get("creationDate")?.jsonPrimitive?.contentOrNull,
                    sizeBytes = variant?.get("size")?.jsonPrimitive?.longOrNull,
                    os = platform?.get("os")?.jsonPrimitive?.contentOrNull,
                    architecture = platform?.get("architecture")?.jsonPrimitive?.contentOrNull,
                    exposedPorts = parseExposedPorts(variant ?: JsonObject(emptyMap()))
                )
            }.getOrNull()
        }
    }

    // ---- Volumes ----------------------------------------------------------------------

    fun parseVolumeList(raw: String): List<VolumeInfo> {
        val root = safeParseArray(raw) ?: return emptyList()
        return root.mapNotNull { element ->
            runCatching {
                val obj = element.jsonObject
                val config = obj["configuration"]?.jsonObject ?: return@runCatching null

                VolumeInfo(
                    name = config["name"]?.jsonPrimitive?.contentOrNull ?: "unknown",
                    driver = config["driver"]?.jsonPrimitive?.contentOrNull,
                    sizeBytes = config["sizeInBytes"]?.jsonPrimitive?.longOrNull,
                    createdAt = config["creationDate"]?.jsonPrimitive?.contentOrNull,
                )
            }.getOrNull()
        }
    }

    // ---- Networks ---------------------------------------------------------------------

    fun parseNetworkList(raw: String): List<NetworkInfo> {
        val root = safeParseArray(raw) ?: return emptyList()

        return root.mapNotNull { el ->
            runCatching {
                val obj = el.jsonObject

                val configuration = obj["configuration"]?.jsonObject
                val status = obj["status"]?.jsonObject

                NetworkInfo(
                    name = configuration?.get("name")?.jsonPrimitive?.contentOrNull ?: "unknown",
                    plugin = configuration?.get("plugin")?.jsonPrimitive?.contentOrNull,
                    gateway = status?.get("ipv4Gateway")?.jsonPrimitive?.contentOrNull,
                    subnet = status?.get("ipv4Subnet")?.jsonPrimitive?.contentOrNull,
                    subnetV6 = status?.get("ipv6Subnet")?.jsonPrimitive?.contentOrNull,
                    mod = configuration?.get("nat")?.jsonPrimitive?.contentOrNull,
                )
            }.getOrNull()
        }
    }

    // ---- System -------------------------------------------------------------------------

    fun parseSystemVersion(raw: String): SystemVersionInfo {
        val root = safeParseArray(raw) ?: return SystemVersionInfo(emptyList())
        val components = root.mapNotNull { el ->
            runCatching {
                val obj = el.jsonObject
                SystemComponentVersion(
                    appName = obj["appName"]?.jsonPrimitive?.contentOrNull ?: "unknown",
                    version = obj["version"]?.jsonPrimitive?.contentOrNull ?: "",
                    buildType = obj["buildType"]?.jsonPrimitive?.contentOrNull,
                    commit = obj["commit"]?.jsonPrimitive?.contentOrNull,
                )
            }.getOrNull()
        }
        return SystemVersionInfo(components)
    }

    private fun safeParseArray(raw: String): JsonArray? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return JsonArray(emptyList())
        return runCatching {
            when (val element: JsonElement = json.parseToJsonElement(trimmed)) {
                is JsonArray -> element
                is JsonObject -> JsonArray(listOf(element))
                else -> JsonArray(emptyList())
            }
        }.getOrNull()
    }

    private fun humanBytes(bytes: Long): String {
        val units = listOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var unitIndex = 0
        while (value >= 1024 && unitIndex < units.lastIndex) {
            value /= 1024
            unitIndex++
        }
        return "%.1f%s".format(value, units[unitIndex])
    }
}
