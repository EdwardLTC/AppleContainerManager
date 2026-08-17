package dev.containermanager.applecontainer.actions.container

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.testFramework.LightVirtualFile
import dev.containermanager.applecontainer.actions.AppleContainerDataKeys
import dev.containermanager.applecontainer.services.ContainerRuntimeService
import dev.containermanager.applecontainer.services.PluginScopeService
import dev.containermanager.applecontainer.util.AppleContainerNotifier
import dev.containermanager.applecontainer.util.ConsoleRunner

class ViewLogsAction : AnAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val selection = e.getData(AppleContainerDataKeys.SELECTED_CONTAINERS)
        e.presentation.isEnabled = selection?.size == 1
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val container = e.getData(AppleContainerDataKeys.SELECTED_CONTAINERS)?.singleOrNull() ?: return
        val cli = ContainerRuntimeService.getInstance(project).cli
        val handler = cli.containers.logsStreaming(container.id, follow = true)
        ConsoleRunner.runInConsole(project, handler, "Logs: ${container.displayName}")
    }
}

class ExecInContainerAction : AnAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val selection = e.getData(AppleContainerDataKeys.SELECTED_CONTAINERS)
        e.presentation.isEnabled = selection?.size == 1
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val container = e.getData(AppleContainerDataKeys.SELECTED_CONTAINERS)?.singleOrNull() ?: return

        val shellCommand = Messages.showInputDialog(
            project,
            "Command to execute inside ${container.displayName}:",
            "Exec in Container",
            Messages.getQuestionIcon(),
            "/bin/sh",
            null,
        ) ?: return

        val cli = ContainerRuntimeService.getInstance(project).cli
        val handler = cli.containers.execStreaming(
            container.id,
            command = listOf(shellCommand),
            interactive = true,
            tty = true,
        )
        ConsoleRunner.runInConsole(project, handler, "Exec: ${container.displayName}")
    }
}

class InspectContainerAction : AnAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val selection = e.getData(AppleContainerDataKeys.SELECTED_CONTAINERS)
        e.presentation.isEnabled = !selection.isNullOrEmpty()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val selected = e.getData(AppleContainerDataKeys.SELECTED_CONTAINERS) ?: return
        PluginScopeService.getInstance(project).launchIo(
            onError = { t -> AppleContainerNotifier.error(project, "Inspect failed", t.message ?: "") },
        ) {
            val json = ContainerRuntimeService.getInstance(project).cli.containers.inspect(selected.map { it.id })
            openJsonPreview(project, "container-inspect-${selected.first().id.take(8)}.json", json)
        }
    }
}

class ViewStatsAction : AnAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val selection = e.getData(AppleContainerDataKeys.SELECTED_CONTAINERS)
        e.presentation.isEnabled = !selection.isNullOrEmpty()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val selected = e.getData(AppleContainerDataKeys.SELECTED_CONTAINERS) ?: return
        val cli = ContainerRuntimeService.getInstance(project).cli
        val handler = cli.containers.statsStreaming(selected.map { it.id })
        ConsoleRunner.runInConsole(project, handler, "Stats")
    }
}

/** Opens a scratch-like read-only preview using IntelliJ's editor (gives JSON folding/formatting for free). */
internal fun openJsonPreview(project: Project, name: String, content: String) {
    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
        val file = LightVirtualFile(name, PlainTextFileType.INSTANCE, content)
        file.isWritable = false
        FileEditorManager.getInstance(project).openFile(file, true)
    }
}
