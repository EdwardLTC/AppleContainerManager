package dev.containermanager.applecontainer.cli.model

/**
 * Structured representation of everything `container run` / `container create` accept.
 * Building this as a typed spec (rather than a raw string) is what lets the Run
 * Configuration UI, quick-run actions, and any future "run from Dockerfile" flow all
 * share one argument builder ([dev.containermanager.applecontainer.cli.ArgBuilders.runArgs]).
 */
data class RunSpec(
    val image: String,
    val arguments: List<String> = emptyList(),
    val name: String? = null,
    val detach: Boolean = true,
    val interactive: Boolean = false,
    val tty: Boolean = false,
    val removeOnExit: Boolean = false,
    val env: List<String> = emptyList(),
    val envFile: String? = null,
    val workdir: String? = null,
    val user: String? = null,
    val cpus: String? = null,
    val memory: String? = null,
    val ports: List<String> = emptyList(),
    val volumes: List<String> = emptyList(),
    val mounts: List<String> = emptyList(),
    val networks: List<String> = emptyList(),
    val dnsServers: List<String> = emptyList(),
    val labels: List<String> = emptyList(),
    val capAdd: List<String> = emptyList(),
    val capDrop: List<String> = emptyList(),
    val entrypoint: String? = null,
    val arch: String? = null,
    val os: String? = null,
    val platform: String? = null,
    val init: Boolean = false,
    val rosetta: Boolean = false,
    val readOnly: Boolean = false,
    val extraArgs: List<String> = emptyList(),
)

data class BuildSpec(
    val contextDir: String,
    val dockerfile: String? = null,
    val tags: List<String> = emptyList(),
    val buildArgs: List<String> = emptyList(),
    val target: String? = null,
    val noCache: Boolean = false,
    val pull: Boolean = false,
    val platform: String? = null,
    val cpus: String? = null,
    val memory: String? = null,
    val labels: List<String> = emptyList(),
    val extraArgs: List<String> = emptyList(),
)

/** Result of a non-streaming CLI invocation. */
data class CliResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
) {
    val isSuccess: Boolean get() = exitCode == 0
}

class CliException(message: String, val result: CliResult? = null) : Exception(message)
