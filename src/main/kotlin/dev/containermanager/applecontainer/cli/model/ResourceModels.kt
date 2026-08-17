package dev.containermanager.applecontainer.cli.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class ImageInfo(
    val reference: String,
    val id: String?,
    val digest: String?,
    val createdAt: String?,
    val sizeBytes: Long?,
    val os: String?,
    val architecture: String?,
    val labels: Map<String, String> = emptyMap(),
    val exposedPorts: List<ExposedPort> = emptyList(),
) {
    val shortId: String get() = (id ?: digest ?: reference).takeLast(12)
}

data class ExposedPort(
    val port: Int,
    val protocol: String = "tcp",
)

data class VolumeInfo(
    val name: String,
    val driver: String?,
    val sizeBytes: Long?,
    val createdAt: String?,
    val labels: Map<String, String> = emptyMap(),
    val inUse: Boolean = false,
){
    override fun toString(): String = name
}

data class NetworkInfo(
    val name: String,
    val plugin: String?,
    val gateway: String?,
    val subnet: String?,
    val subnetV6: String?,
    val mod: String?,
    val labels: Map<String, String> = emptyMap(),
)

data class RegistryLoginInfo(
    val host: String,
    val username: String?,
)

data class SystemStatusInfo(
    val running: Boolean,
    val raw: String,
)

data class SystemVersionInfo(
    val components: List<SystemComponentVersion>,
)

data class SystemComponentVersion(
    val appName: String,
    val version: String,
    val buildType: String?,
    val commit: String?,
)

data class DiskUsageInfo(
    val images: DiskUsageRow,
    val containers: DiskUsageRow,
    val volumes: DiskUsageRow,
)

data class DiskUsageRow(
    val total: Int,
    val active: Int,
    val sizeBytes: Long,
    val reclaimableBytes: Long,
)

data class BuilderStatusInfo(
    val running: Boolean,
    val containerId: String?,
    val raw: String,
)

data class MachineInfo(
    val id: String,
    val isDefault: Boolean,
    val status: String?,
)

data class KubernetesClusterInfo(
    val name: String,
    val status: String?,
    val nodeImage: String?,
)

@Serializable
data class SystemVersionRow(
    @SerialName("appName") val appName: String? = null,
    @SerialName("version") val version: String? = null,
    @SerialName("buildType") val buildType: String? = null,
    @SerialName("commit") val commit: String? = null,
)
