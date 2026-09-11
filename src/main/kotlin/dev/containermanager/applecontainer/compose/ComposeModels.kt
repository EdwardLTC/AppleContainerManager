package dev.containermanager.applecontainer.compose

import java.io.File

/** Parsed Compose project, normalized for the Apple Container runtime. */
data class ComposeFile(
    val projectName: String,
    val composeDir: File,
    val services: Map<String, ComposeService>,
    val volumes: Map<String, ComposeVolume> = emptyMap(),
    val networks: Map<String, ComposeNetwork> = emptyMap(),
    val warnings: List<String> = emptyList(),
)

data class ComposeService(
    val name: String,
    val image: String?,
    val build: ComposeBuild?,
    val command: List<String>,
    val entrypoint: String?,
    val environment: List<String>,
    val envFiles: List<String>,
    val ports: List<String>,
    val volumes: List<String>,
    val dependsOn: List<String>,
    val networks: List<String>,
    val platform: String?,
    val workingDir: String?,
    val user: String?,
    val memory: String?,
    val cpus: String?,
    val labels: List<String>,
    val networkMode: String?,
)

data class ComposeBuild(
    val context: String,
    val dockerfile: String?,
    val args: List<String>,
    val target: String?,
)

data class ComposeVolume(
    val name: String,
    val external: Boolean,
    val runtimeName: String?,
)

data class ComposeNetwork(
    val name: String,
    val external: Boolean,
    val internal: Boolean,
    val runtimeName: String?,
)

class ComposeException(message: String, cause: Throwable? = null) : Exception(message, cause)
