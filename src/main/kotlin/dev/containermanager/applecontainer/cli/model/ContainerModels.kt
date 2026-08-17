package dev.containermanager.applecontainer.cli.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Domain-level representation of a container, decoupled from the raw CLI JSON shape.
 *
 * The `container` CLI's `--format json` output is mapped onto this type inside
 * [dev.containermanager.applecontainer.cli.parse.ContainerJsonMapper] so that everything above the
 * CLI layer (services, UI, run configurations) works with a stable model even if the
 * upstream JSON schema shifts between `container` releases.
 */
data class ContainerInfo(
    val id: String,
    val name: String?,
    val image: String,
    val status: ContainerStatus,
    val createdAt: String?,
    val command: List<String> = emptyList(),
    val cpus: Int? = null,
    val memory: String? = null,
    val ports: List<PortMapping> = emptyList(),
    val networks: List<String> = emptyList(),
    val labels: Map<String, String> = emptyMap(),
    val arch: String? = null,
    val os: String? = null,
    val platform: String? = null,
) {
    val displayName: String get() = name ?: id.take(12)
}

enum class ContainerStatus {
    RUNNING, STOPPED, CREATED, EXITED, UNKNOWN;

    companion object {
        fun parse(raw: String?): ContainerStatus = when (raw?.trim()?.lowercase()) {
            "running" -> RUNNING
            "stopped" -> STOPPED
            "created" -> CREATED
            "exited" -> EXITED
            else -> UNKNOWN
        }
    }
}

data class PortMapping(
    val hostIp: String?,
    val hostPort: Int,
    val containerPort: Int,
    val protocol: String = "tcp",
) {
    override fun toString(): String =
        "${hostIp?.let { "$it:" } ?: ""}$hostPort->$containerPort/$protocol"
}

data class ContainerStats(
    val id: String,
    val cpuPercent: Double?,
    val memoryUsageBytes: Long?,
    val memoryLimitBytes: Long?,
    val networkRxBytes: Long?,
    val networkTxBytes: Long?,
    val blockReadBytes: Long?,
    val blockWriteBytes: Long?,
    val pids: Int?,
)

/** Raw JSON row shapes as emitted by `container list --format json` (best-effort; see README). */
@Serializable
data class ContainerListRow(
    @SerialName("id") val id: String? = null,
    @SerialName("configuration") val configuration: ContainerConfigurationRow? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("networks") val networks: List<ContainerNetworkRow>? = null,
)

@Serializable
data class ContainerConfigurationRow(
    @SerialName("id") val id: String? = null,
    @SerialName("image") val image: ImageReferenceRow? = null,
    @SerialName("labels") val labels: Map<String, String>? = null,
    @SerialName("resources") val resources: ResourcesRow? = null,
    @SerialName("platform") val platform: PlatformRow? = null,
    @SerialName("initProcess") val initProcess: InitProcessRow? = null,
)

@Serializable
data class ImageReferenceRow(
    @SerialName("reference") val reference: String? = null,
)

@Serializable
data class ResourcesRow(
    @SerialName("cpus") val cpus: Int? = null,
    @SerialName("memoryInBytes") val memoryInBytes: Long? = null,
)

@Serializable
data class PlatformRow(
    @SerialName("os") val os: String? = null,
    @SerialName("architecture") val architecture: String? = null,
)

@Serializable
data class InitProcessRow(
    @SerialName("arguments") val arguments: List<String>? = null,
    @SerialName("executable") val executable: String? = null,
)

@Serializable
data class ContainerNetworkRow(
    @SerialName("network") val network: String? = null,
    @SerialName("address") val address: String? = null,
)
