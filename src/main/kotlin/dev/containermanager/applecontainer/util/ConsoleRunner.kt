package dev.containermanager.applecontainer.util

import com.intellij.execution.RunContentExecutor
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project

/**
 * Streams a live [OSProcessHandler] into IntelliJ's standard Run tool window, reusing the
 * platform's own console (syntax-aware, ANSI-colored, with a built-in Stop button and rerun
 * affordance) instead of building a bespoke output widget. Used for `run`, `logs -f`, `build`,
 * `pull`, `push`, `exec`, and `stats` \u2014 anything that streams.
 */
object ConsoleRunner {

    fun runInConsole(
        project: Project,
        handler: OSProcessHandler,
        title: String,
        onTerminate: (() -> Unit)? = null,
    ) {
        ApplicationManager.getApplication().invokeLater {
            val executor = RunContentExecutor(project, handler)
                .withTitle(title)
                .withActivateToolWindow(true)
                .withStop({ handler.destroyProcess() }) {
                    !handler.isProcessTerminated
                }

            if (onTerminate != null) {
                handler.addProcessListener(object : ProcessListener {
                    override fun processTerminated(event: ProcessEvent) {
                        onTerminate()
                    }
                })
            }

            executor.run()
        }
    }
}