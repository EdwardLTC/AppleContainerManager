package dev.containermanager.applecontainer.cli

import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import dev.containermanager.applecontainer.cli.model.BuildSpec
import dev.containermanager.applecontainer.cli.model.ImageInfo
import dev.containermanager.applecontainer.cli.parse.JsonMapper

/** Wraps `container image ...` and `container build`. */
class ImageCommands(private val executor: CliExecutor) {

    suspend fun list(verbose: Boolean = false): List<ImageInfo> {
        val args = buildList {
            addAll(listOf("image", "list"))
            if (verbose) add("--verbose")
            addAll(listOf("--format", "json"))
        }
        val result = executor.execOrThrow(args)
        return JsonMapper.parseImageList(result.stdout)
    }

    suspend fun inspect(references: List<String>): String =
        executor.execOrThrow(listOf("image", "inspect") + references).stdout

    /** Live handler; caller streams progress into a console (build/push/pull all report progress on stdout). */
    fun pullStreaming(reference: String, platform: String? = null): OSProcessHandler {
        val args = buildList {
            addAll(listOf("image", "pull"))
            addAll(listOf("--progress", "plain"))
            platform?.let { addAll(listOf("--platform", it)) }
            add(reference)
        }
        return executor.createStreamingHandler(args)
    }

    fun pushStreaming(reference: String, platform: String? = null): OSProcessHandler {
        val args = buildList {
            addAll(listOf("image", "push"))
            addAll(listOf("--progress", "plain"))
            platform?.let { addAll(listOf("--platform", it)) }
            add(reference)
        }
        return executor.createStreamingHandler(args)
    }

    fun buildStreaming(spec: BuildSpec): OSProcessHandler =
        executor.createStreamingHandler(ArgBuilders.buildArgs(spec))

    suspend fun save(references: List<String>, outputPath: String, platform: String? = null) {
        val args = buildList {
            addAll(listOf("image", "save"))
            addAll(listOf("--output", outputPath))
            platform?.let { addAll(listOf("--platform", it)) }
            addAll(references)
        }
        executor.execOrThrow(args)
    }

    suspend fun load(inputPath: String, force: Boolean = false) {
        val args = buildList {
            addAll(listOf("image", "load", "--input", inputPath))
            if (force) add("--force")
        }
        executor.execOrThrow(args)
    }

    suspend fun tag(source: String, target: String) {
        executor.execOrThrow(listOf("image", "tag", source, target))
    }

    suspend fun delete(references: List<String>, all: Boolean = false, force: Boolean = false) {
        val args = buildList {
            addAll(listOf("image", "delete"))
            if (all) add("--all")
            if (force) add("--force")
            addAll(references)
        }
        executor.execOrThrow(args)
    }

    suspend fun prune(all: Boolean = false): String {
        val args = buildList {
            addAll(listOf("image", "prune"))
            if (all) add("--all")
        }
        return executor.execOrThrow(args).stdout
    }

    suspend fun buildAndAwait(spec: BuildSpec, onOutputLine: ((String) -> Unit)? = null): Int {
        val handler = buildStreaming(spec)
        if (onOutputLine != null) {
            handler.addProcessListener(object : ProcessListener {
                override fun onTextAvailable(event: ProcessEvent, outputType: com.intellij.openapi.util.Key<*>) {
                    onOutputLine(event.text)
                }
            })
        }
        return executor.awaitCompletion(handler)
    }

}
