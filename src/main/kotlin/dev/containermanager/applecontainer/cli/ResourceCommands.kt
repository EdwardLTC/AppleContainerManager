package dev.containermanager.applecontainer.cli

import dev.containermanager.applecontainer.cli.model.BuilderStatusInfo
import dev.containermanager.applecontainer.cli.model.NetworkInfo
import dev.containermanager.applecontainer.cli.model.RegistryLoginInfo
import dev.containermanager.applecontainer.cli.model.VolumeInfo
import dev.containermanager.applecontainer.cli.parse.JsonMapper

/** Wraps `container volume ...`. */
class VolumeCommands(private val executor: CliExecutor) {

    suspend fun list(): List<VolumeInfo> {
        val result = executor.execOrThrow(listOf("volume", "list", "--format", "json"))
        return JsonMapper.parseVolumeList(result.stdout)
    }

    suspend fun inspect(names: List<String>): String =
        executor.execOrThrow(listOf("volume", "inspect") + names).stdout

    suspend fun create(
        name: String,
        sizeBytes: String? = null,
        labels: List<String> = emptyList(),
        opts: List<String> = emptyList()
    ) {
        val args = buildList {
            addAll(listOf("volume", "create"))
            sizeBytes?.let { addAll(listOf("-s", it)) }
            labels.forEach { addAll(listOf("--label", it)) }
            opts.forEach { addAll(listOf("--opt", it)) }
            add(name)
        }
        executor.execOrThrow(args)
    }

    suspend fun delete(names: List<String>, all: Boolean = false) {
        val args = buildList {
            addAll(listOf("volume", "delete"))
            if (all) add("--all")
            addAll(names)
        }
        executor.execOrThrow(args)
    }

    suspend fun prune(): String = executor.execOrThrow(listOf("volume", "prune")).stdout
}

/** Wraps `container network ...` (macOS 26+). */
class NetworkCommands(private val executor: CliExecutor) {

    suspend fun list(): List<NetworkInfo> {
        val result = executor.execOrThrow(listOf("network", "list", "--format", "json"))
        return JsonMapper.parseNetworkList(result.stdout)
    }

    suspend fun inspect(names: List<String>): String =
        executor.execOrThrow(listOf("network", "inspect") + names).stdout

    suspend fun create(
        name: String,
        internal: Boolean = false,
        subnet: String? = null,
        subnetV6: String? = null,
        labels: List<String> = emptyList(),
    ) {
        val args = buildList {
            addAll(listOf("network", "create"))
            if (internal) add("--internal")
            subnet?.let { addAll(listOf("--subnet", it)) }
            subnetV6?.let { addAll(listOf("--subnet-v6", it)) }
            labels.forEach { addAll(listOf("--label", it)) }
            add(name)
        }
        executor.execOrThrow(args)
    }

    suspend fun delete(names: List<String>, all: Boolean = false) {
        val args = buildList {
            addAll(listOf("network", "delete"))
            if (all) add("--all")
            addAll(names)
        }
        executor.execOrThrow(args)
    }

    suspend fun prune(): String = executor.execOrThrow(listOf("network", "prune")).stdout
}

/** Wraps `container registry ...`. */
class RegistryCommands(private val executor: CliExecutor) {

    suspend fun login(server: String, username: String?, passwordStdin: String?) {
        val args = buildList {
            addAll(listOf("registry", "login"))
            username?.let { addAll(listOf("--username", it)) }
            if (passwordStdin != null) add("--password-stdin")
            add(server)
        }
        executor.execOrThrow(args, stdin = passwordStdin)
    }

    suspend fun logout(server: String) {
        executor.execOrThrow(listOf("registry", "logout", server))
    }

    suspend fun list(): List<RegistryLoginInfo> {
        val result = executor.execOrThrow(listOf("registry", "list", "--format", "json"))
        val root = runCatching { JsonMapper.json.parseToJsonElement(result.stdout) }.getOrNull() ?: return emptyList()
        return emptyList() // Schema isn't documented; surfaced via raw output in the UI instead.
    }
}

/** Wraps `container builder ...` (BuildKit-based builder lifecycle). */
class BuilderCommands(private val executor: CliExecutor) {

    suspend fun start(cpus: String? = null, memory: String? = null) {
        val args = buildList {
            addAll(listOf("builder", "start"))
            cpus?.let { addAll(listOf("--cpus", it)) }
            memory?.let { addAll(listOf("--memory", it)) }
        }
        executor.execOrThrow(args)
    }

    suspend fun status(): BuilderStatusInfo {
        val result = executor.execOrThrow(listOf("builder", "status"))
        return BuilderStatusInfo(
            running = result.stdout.contains("running", ignoreCase = true),
            containerId = null,
            raw = result.stdout,
        )
    }

    suspend fun stop() {
        executor.execOrThrow(listOf("builder", "stop"))
    }

    suspend fun delete(force: Boolean = false) {
        val args = buildList {
            addAll(listOf("builder", "delete"))
            if (force) add("--force")
        }
        executor.execOrThrow(args)
    }
}
