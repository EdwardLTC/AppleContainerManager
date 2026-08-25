package dev.containermanager.applecontainer.compose

/**
 * A deliberately small subset of the Compose spec — enough to cover the fields that map
 * cleanly onto `container build` / `container run` (which is all Apple's `container` CLI has;
 * there is no native `container compose` command). Anything Compose supports that has no
 * equivalent concept in the `container` CLI (healthchecks, deploy/replicas, profiles, secrets,
 * configs, extends, ...) is intentionally not modeled here rather than silently mis-mapped.
 */
data class ComposeFile(
    val services: Map<String, ComposeService>,
)

data class ComposeService(
    val name: String,
    val image: String?,
    val build: ComposeBuild?,
    val containerName: String?,
    val command: List<String>,
    val environment: List<String>,
    val ports: List<String>,
    val volumes: List<String>,
    val dependsOn: List<String>,
    val networks: List<String>,
) {
    /** A service must resolve to a runnable image one way or another. */
    fun requiresBuild(): Boolean = build != null
}

data class ComposeBuild(
    val context: String,
    val dockerfile: String?,
    val args: List<String>,
    val target: String?,
)

class ComposeException(message: String) : Exception(message)