package dev.containermanager.applecontainer.run

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.project.Project
import dev.containermanager.applecontainer.services.ContainerRuntimeService

/**
 * Builds the OSProcessHandler for a `container run` invocation and wraps it in a console,
 * making the run configuration behave like any built-in IntelliJ run target: Stop button,
 * rerun, output in the Run tool window, and full compatibility with the platform's execution UI.
 */
class ContainerCommandLineState(
    environment: ExecutionEnvironment,
    private val options: ContainerRunOptions,
) : RunProfileState {

    private val project: Project = environment.project

    override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult {
        val cli = ContainerRuntimeService.getInstance(project).cli
        val handler = cli.containers.runStreaming(options.toRunSpec())

        val consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(project)
        val console = consoleBuilder.console
        console.attachToProcess(handler)
        handler.startNotify()

        return DefaultExecutionResult(console, handler)
    }
}
