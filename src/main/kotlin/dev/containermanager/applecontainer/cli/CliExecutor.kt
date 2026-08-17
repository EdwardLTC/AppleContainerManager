package dev.containermanager.applecontainer.cli

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.diagnostic.thisLogger
import dev.containermanager.applecontainer.cli.model.CliException
import dev.containermanager.applecontainer.cli.model.CliResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Executes the `container` binary off the EDT and never blocks the UI thread.
 *
 * Two execution modes are exposed:
 *  - [exec]: fire-and-wait, output fully captured, used for list/inspect/metadata calls.
 *  - [createStreamingHandler]: returns a live [OSProcessHandler] the caller attaches a
 *    ConsoleView / listener to, used for `run`, `logs -f`, `build`, `pull`, `exec`, `stats`.
 *
 * This is the single choke point that knows how to invoke the CLI; every command-family
 * class (ContainerCommands, ImageCommands, ...) is built on top of it, and it's what would be
 * swapped out (or parameterized) if this plugin ever needed to shell out differently.
 */
class CliExecutor(
    private val binaryPathProvider: () -> String?,
    private val workDirectory: String? = null,
) {
    private val logger = thisLogger()

    /** Runs the command to completion and captures stdout/stderr. Suspends off the EDT. */
    suspend fun exec(args: List<String>, stdin: String? = null, timeoutMs: Long = 60_000): CliResult =
        withContext(Dispatchers.IO) {
            val binary = requireBinary()
            val commandLine = buildCommandLine(binary, args)
            logger.debug("Executing: ${commandLine.commandLineString}")

            val handler = CapturingProcessHandler(commandLine)
            if (stdin != null) {
                handler.processInput.use { it.write(stdin.toByteArray()) }
            }
            val output = handler.runProcess(timeoutMs.toInt())
            if (output.isTimeout) {
                throw CliException("Command timed out after ${timeoutMs}ms: container ${args.joinToString(" ")}")
            }
            CliResult(output.exitCode, output.stdout, output.stderr)
        }

    /** Same as [exec] but throws if the process exits non-zero, surfacing stderr in the message. */
    suspend fun execOrThrow(args: List<String>, stdin: String? = null, timeoutMs: Long = 60_000): CliResult {
        val result = exec(args, stdin, timeoutMs)
        if (!result.isSuccess) {
            val message = result.stderr.ifBlank { result.stdout }.ifBlank { "Exit code ${result.exitCode}" }
            throw CliException("container ${args.joinToString(" ")} failed: $message", result)
        }
        return result
    }

    /**
     * Creates (but does not start) a process handler suitable for streaming output into a
     * ConsoleView or a custom listener. Caller owns the handler's lifecycle (startNotify /
     * destroyProcess). Building the [GeneralCommandLine] itself is cheap and safe to call from
     * the EDT; only the process I/O happens asynchronously once started.
     */
    fun createStreamingHandler(args: List<String>): OSProcessHandler {
        val binary = requireBinary()
        val commandLine = buildCommandLine(binary, args)
        logger.debug("Streaming: ${commandLine.commandLineString}")
        return OSProcessHandler(commandLine)
    }

    /** Await process termination as a suspend function, useful for one-shot streaming commands. */
    suspend fun awaitCompletion(handler: OSProcessHandler): Int = suspendCancellableCoroutine { cont ->
        handler.addProcessListener(object : ProcessAdapter() {
            override fun processTerminated(event: ProcessEvent) {
                if (cont.isActive) cont.resume(event.exitCode)
            }
        })
        cont.invokeOnCancellation {
            try {
                handler.destroyProcess()
            } catch (_: Exception) {
                // best-effort cleanup
            }
        }
        if (!handler.isStartNotified) handler.startNotify()
    }

    private fun requireBinary(): String =
        binaryPathProvider() ?: throw CliException(
            "The `container` CLI could not be found. Configure its path in " +
                "Settings \u2192 Tools \u2192 Apple Container Manager."
        )

    private fun buildCommandLine(binary: String, args: List<String>): GeneralCommandLine =
        GeneralCommandLine(listOf(binary) + args).apply {
            workDirectory?.let { withWorkDirectory(it) }
            charset = Charsets.UTF_8
        }
}

/** Converts a checked cancellation into coroutine cancellation instead of a reported error. */
internal fun rethrowCancellation(t: Throwable): Nothing {
    if (t is CancellationException) throw t
    throw CliException(t.message ?: "Unknown error", null)
}
