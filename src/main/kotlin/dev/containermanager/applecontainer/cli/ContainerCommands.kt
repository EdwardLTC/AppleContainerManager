package dev.containermanager.applecontainer.cli

import com.intellij.execution.process.OSProcessHandler
import dev.containermanager.applecontainer.cli.model.ContainerInfo
import dev.containermanager.applecontainer.cli.model.RunSpec
import dev.containermanager.applecontainer.cli.parse.JsonMapper

/** Wraps `container run|create|start|stop|kill|delete|list|exec|logs|inspect|stats|cp|export|prune`. */
class ContainerCommands(private val executor: CliExecutor) {

    suspend fun list(all: Boolean = true): List<ContainerInfo> {
        val args = buildList {
            add("list")
            if (all) add("--all")
            addAll(listOf("--format", "json"))
        }
        val result = executor.execOrThrow(args)
        return JsonMapper.parseContainerList(result.stdout)
    }

    suspend fun inspect(ids: List<String>): String =
        executor.execOrThrow(listOf("inspect") + ids).stdout

    suspend fun create(spec: RunSpec): String {
        val result = executor.execOrThrow(ArgBuilders.runArgs(spec, subcommand = "create"))
        return result.stdout.trim()
    }

    /** Returns a live handler; the caller attaches a ConsoleView and calls startNotify(). */
    fun runStreaming(spec: RunSpec): OSProcessHandler =
        executor.createStreamingHandler(ArgBuilders.runArgs(spec, subcommand = "run"))

    suspend fun start(id: String, attach: Boolean = false, interactive: Boolean = false) {
        val args = buildList {
            add("start")
            if (attach) add("--attach")
            if (interactive) add("--interactive")
            add(id)
        }
        executor.execOrThrow(args)
    }

    suspend fun stop(ids: List<String>, all: Boolean = false, signal: String? = null, timeSeconds: Int? = null) {
        val args = buildList {
            add("stop")
            if (all) add("--all")
            signal?.let { addAll(listOf("--signal", it)) }
            timeSeconds?.let { addAll(listOf("--time", it.toString())) }
            addAll(ids)
        }
        executor.execOrThrow(args)
    }

    suspend fun kill(ids: List<String>, all: Boolean = false, signal: String? = null) {
        val args = buildList {
            add("kill")
            if (all) add("--all")
            signal?.let { addAll(listOf("--signal", it)) }
            addAll(ids)
        }
        executor.execOrThrow(args)
    }

    suspend fun delete(ids: List<String>, all: Boolean = false, force: Boolean = false) {
        val args = buildList {
            add("delete")
            if (all) add("--all")
            if (force) add("--force")
            addAll(ids)
        }
        executor.execOrThrow(args)
    }

    suspend fun prune(): String = executor.execOrThrow(listOf("prune")).stdout

    /** Live handler for `logs -f`. Caller pipes stdout into a console. */
    fun logsStreaming(id: String, follow: Boolean = true, tail: Int? = null, boot: Boolean = false): OSProcessHandler {
        val args = buildList {
            add("logs")
            if (follow) add("--follow")
            if (boot) add("--boot")
            tail?.let { addAll(listOf("-n", it.toString())) }
            add(id)
        }
        return executor.createStreamingHandler(args)
    }

    /** Live handler for `exec`; wire this into a Terminal tab for an interactive shell. */
    fun execStreaming(
        id: String,
        command: List<String>,
        interactive: Boolean = true,
        tty: Boolean = true,
        workdir: String? = null,
        user: String? = null,
        env: List<String> = emptyList(),
    ): OSProcessHandler {
        val args = buildList {
            add("exec")
            if (interactive) add("--interactive")
            if (tty) add("--tty")
            workdir?.let { addAll(listOf("--workdir", it)) }
            user?.let { addAll(listOf("--user", it)) }
            env.forEach { addAll(listOf("--env", it)) }
            add(id)
            addAll(command)
        }
        return executor.createStreamingHandler(args)
    }

    suspend fun statsSnapshot(ids: List<String> = emptyList()): String {
        val args = buildList {
            add("stats")
            add("--no-stream")
            addAll(listOf("--format", "json"))
            addAll(ids)
        }
        return executor.execOrThrow(args).stdout
    }

    fun statsStreaming(ids: List<String> = emptyList()): OSProcessHandler {
        val args = buildList {
            add("stats")
            addAll(ids)
        }
        return executor.createStreamingHandler(args)
    }

    suspend fun copy(source: String, destination: String) {
        executor.execOrThrow(listOf("copy", source, destination))
    }

    suspend fun export(id: String, outputPath: String) {
        executor.execOrThrow(listOf("export", "-o", outputPath, id))
    }
}
