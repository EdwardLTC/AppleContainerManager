package dev.containermanager.applecontainer.actions.volume

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import dev.containermanager.applecontainer.actions.AppleContainerDataKeys
import dev.containermanager.applecontainer.actions.BaseCliAction
import dev.containermanager.applecontainer.actions.container.openJsonPreview
import dev.containermanager.applecontainer.services.ContainerRuntimeService
import dev.containermanager.applecontainer.services.PluginScopeService
import dev.containermanager.applecontainer.util.AppleContainerNotifier

class CreateVolumeAction : AnAction() {
    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled =
            ContainerRuntimeService.getInstance(event.project ?: return).isServicesRunning()
    }
    
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val name = Messages.showInputDialog(
            project, "Volume name:", "Create Volume", Messages.getQuestionIcon(),
        ) ?: return
        if (name.isBlank()) return

        PluginScopeService.getInstance(project).launchIo(
            onError = { t -> AppleContainerNotifier.error(project, "Create volume failed", t.message ?: "") },
        ) {
            ContainerRuntimeService.getInstance(project).cli.volumes.create(name.trim())
            ContainerRuntimeService.getInstance(project).requestRefresh()
        }
    }
}

class DeleteVolumeAction : BaseCliAction() {
    override fun update(e: AnActionEvent) {
        val selection = e.getData(AppleContainerDataKeys.SELECTED_VOLUMES)
        e.presentation.isEnabled = !selection.isNullOrEmpty()
    }

    override fun confirmationMessage(e: AnActionEvent): String {
        val selected = e.getData(AppleContainerDataKeys.SELECTED_VOLUMES).orEmpty()
        return "Delete ${selected.size} volume(s)? Data will be permanently lost."
    }

    override suspend fun performCli(project: Project, e: AnActionEvent) {
        val selected = e.getData(AppleContainerDataKeys.SELECTED_VOLUMES) ?: return
        ContainerRuntimeService.getInstance(project).cli.volumes.delete(selected.map { it.name })
    }
}

class PruneVolumesAction : BaseCliAction() {
    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled =
            ContainerRuntimeService.getInstance(event.project ?: return).isServicesRunning()
    }

    override fun confirmationMessage(e: AnActionEvent): String =
        "Remove all volumes not referenced by any container? Data will be permanently lost."

    override suspend fun performCli(project: Project, e: AnActionEvent) {
        ContainerRuntimeService.getInstance(project).cli.volumes.prune()
    }
}

class InspectVolumeAction : AnAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val selection = e.getData(AppleContainerDataKeys.SELECTED_VOLUMES)
        e.presentation.isEnabled = !selection.isNullOrEmpty()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val selected = e.getData(AppleContainerDataKeys.SELECTED_VOLUMES) ?: return
        PluginScopeService.getInstance(project).launchIo(
            onError = { t -> AppleContainerNotifier.error(project, "Inspect failed", t.message ?: "") },
        ) {
            val json = ContainerRuntimeService.getInstance(project).cli.volumes.inspect(selected.map { it.name })
            openJsonPreview(project, "volume-inspect.json", json)
        }
    }
}
