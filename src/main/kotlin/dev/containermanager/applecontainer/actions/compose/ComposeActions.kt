package dev.containermanager.applecontainer.actions.compose

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import dev.containermanager.applecontainer.compose.ComposeException
import dev.containermanager.applecontainer.compose.ComposeFile
import dev.containermanager.applecontainer.compose.ComposeOrchestrator
import dev.containermanager.applecontainer.compose.ComposeParser
import dev.containermanager.applecontainer.services.ContainerRuntimeService
import dev.containermanager.applecontainer.services.PluginScopeService
import dev.containermanager.applecontainer.util.AppleContainerNotifier
import dev.containermanager.applecontainer.util.ConsoleRunner
import dev.containermanager.applecontainer.util.VirtualProcessHandler
import kotlinx.coroutines.Job
import java.io.File

internal val COMPOSE_FILE_NAMES = setOf(
    "compose.yml",
    "compose.yaml",
    "docker-compose.yml",
    "docker-compose.yaml",
    "apple-compose.yml",
    "apple-compose.yaml"
)

internal fun composeFileFrom(e: AnActionEvent): VirtualFile? {
    val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return null
    return file.takeIf { it.name in COMPOSE_FILE_NAMES }
}

/** Slug used to namespace built image tags and container names, mirroring `docker compose`'s own convention. */
private fun projectSlug(composeFile: VirtualFile): String =
    (composeFile.parent?.name ?: "compose").lowercase().replace(Regex("[^a-z0-9_-]"), "-")

class ComposeUpAction : AnAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = composeFileFrom(e) != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = composeFileFrom(e) ?: return
        runCompose(project, file, title = "Compose: up") { orchestrator, compose, onLine ->
            orchestrator.up(compose, onLine)
        }
    }
}

class ComposeDownAction : AnAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = composeFileFrom(e) != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = composeFileFrom(e) ?: return
        runCompose(project, file, title = "Compose: down") { orchestrator, compose, onLine ->
            orchestrator.down(compose, onLine)
        }
    }
}

/** Shared plumbing: parse the file, host output in a [VirtualProcessHandler], run [action] off the EDT. */
internal fun runCompose(
    project: Project,
    file: VirtualFile,
    title: String,
    action: suspend (ComposeOrchestrator, ComposeFile, (String) -> Unit) -> Unit,
) {
    val handler = VirtualProcessHandler()
    ConsoleRunner.runInConsole(project, handler, title)

    var job: Job? = null
    handler.onStopRequested = { job?.cancel() }

    job = PluginScopeService.getInstance(project).launchIo(
        onError = { t ->
            val message = if (t is ComposeException) t.message ?: "Compose failed" else (t.message ?: "Compose failed")
            handler.printlnError(message)
            handler.finish(1)
            AppleContainerNotifier.error(project, "Apple Container Compose", message)
        },
    ) {
        val compose = ComposeParser.parse(File(file.path))
        val orchestrator = ComposeOrchestrator(ContainerRuntimeService.getInstance(project).cli, projectSlug(file))
        action(orchestrator, compose) { line -> handler.println(line) }
        handler.println("\nDone.")
        handler.finish(0)
        ContainerRuntimeService.getInstance(project).requestRefresh()
    }
}