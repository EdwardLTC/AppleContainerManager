package dev.containermanager.applecontainer.cli

import dev.containermanager.applecontainer.cli.model.SystemStatusInfo
import dev.containermanager.applecontainer.cli.model.SystemVersionInfo
import dev.containermanager.applecontainer.cli.parse.JsonMapper

/** Wraps `container system ...` (apiserver lifecycle, logs, DNS, kernel, disk usage). */
class SystemCommands(private val executor: CliExecutor) {

    suspend fun start(enableKernelInstall: Boolean? = null, timeoutSeconds: Int? = null) {
        val args = buildList {
            addAll(listOf("system", "start"))
            when (enableKernelInstall) {
                true -> add("--enable-kernel-install")
                false -> add("--disable-kernel-install")
                null -> Unit
            }
            timeoutSeconds?.let { addAll(listOf("--timeout", it.toString())) }
        }
        executor.execOrThrow(args, timeoutMs = 120_000)
    }

    suspend fun stop() {
        executor.execOrThrow(listOf("system", "stop"))
    }

    suspend fun status(): SystemStatusInfo {
        val result = executor.exec(listOf("system", "status"))
        return SystemStatusInfo(running = result.isSuccess, raw = result.stdout.ifBlank { result.stderr })
    }

    suspend fun version(): SystemVersionInfo {
        val result = executor.execOrThrow(listOf("system", "version", "--format", "json"))
        return JsonMapper.parseSystemVersion(result.stdout)
    }

    suspend fun df(): String = executor.execOrThrow(listOf("system", "df")).stdout

    suspend fun logs(last: String = "5m"): String =
        executor.execOrThrow(listOf("system", "logs", "--last", last)).stdout

    suspend fun dnsList(): String = executor.execOrThrow(listOf("system", "dns", "list")).stdout

    suspend fun dnsCreate(domain: String, localhost: String? = null) {
        val args = buildList {
            addAll(listOf("system", "dns", "create"))
            localhost?.let { addAll(listOf("--localhost", it)) }
            add(domain)
        }
        executor.execOrThrow(args)
    }

    suspend fun dnsDelete(domain: String) {
        executor.execOrThrow(listOf("system", "dns", "delete", domain))
    }

    suspend fun kernelSetRecommended() {
        executor.execOrThrow(listOf("system", "kernel", "set", "--recommended"), timeoutMs = 180_000)
    }
}

/** Wraps `container machine ...` (`m` alias). Lightweight VM lifecycle around an image. */
class MachineCommands(private val executor: CliExecutor) {

    suspend fun list(): String = executor.execOrThrow(listOf("machine", "list", "--format", "json")).stdout

    suspend fun create(image: String, name: String? = null, setDefault: Boolean = false, noBoot: Boolean = false) {
        val args = buildList {
            addAll(listOf("machine", "create"))
            name?.let { addAll(listOf("--name", it)) }
            if (setDefault) add("--set-default")
            if (noBoot) add("--no-boot")
            add(image)
        }
        executor.execOrThrow(args, timeoutMs = 120_000)
    }

    suspend fun stop(id: String? = null) {
        executor.execOrThrow(listOf("machine", "stop") + listOfNotNull(id))
    }

    suspend fun delete(id: String) {
        executor.execOrThrow(listOf("machine", "delete", id))
    }

    suspend fun setDefault(id: String) {
        executor.execOrThrow(listOf("machine", "set-default", id))
    }
}

/** Wraps `container k8s ...` (experimental single-node Kubernetes cluster management). */
class K8sCommands(private val executor: CliExecutor) {

    suspend fun list(): String = executor.execOrThrow(listOf("k8s", "list")).stdout

    suspend fun create(name: String? = null, nodeImage: String? = null, remove: Boolean = false) {
        val args = buildList {
            addAll(listOf("k8s", "create"))
            name?.let { addAll(listOf("--name", it)) }
            nodeImage?.let { addAll(listOf("--node-image", it)) }
            if (remove) add("--rm")
        }
        executor.execOrThrow(args, timeoutMs = 180_000)
    }

    suspend fun start(name: String? = null) {
        executor.execOrThrow(listOf("k8s", "start") + (name?.let { listOf("--name", it) } ?: emptyList()))
    }

    suspend fun delete(name: String? = null) {
        executor.execOrThrow(listOf("k8s", "delete") + (name?.let { listOf("--name", it) } ?: emptyList()))
    }

    suspend fun loadImage(reference: String, name: String? = null, platform: String? = null) {
        val args = buildList {
            addAll(listOf("k8s", "load-image"))
            name?.let { addAll(listOf("--name", it)) }
            platform?.let { addAll(listOf("--platform", it)) }
            add(reference)
        }
        executor.execOrThrow(args, timeoutMs = 120_000)
    }

    suspend fun writeConfig(name: String? = null, kubeconfig: String? = null) {
        val args = buildList {
            addAll(listOf("k8s", "write-config"))
            name?.let { addAll(listOf("--name", it)) }
            kubeconfig?.let { addAll(listOf("--kubeconfig", it)) }
        }
        executor.execOrThrow(args)
    }
}
