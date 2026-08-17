package dev.containermanager.applecontainer.actions.network

import com.intellij.icons.AllIcons
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

class CreateNetworkAction : AnAction("Create Network\u2026", "Create a new network", AllIcons.General.Add) {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val name = Messages.showInputDialog(
            project, "Network name:", "Create Network", Messages.getQuestionIcon(),
        ) ?: return
        if (name.isBlank()) return

        PluginScopeService.getInstance(project).launchIo(
            onError = { t -> AppleContainerNotifier.error(project, "Create network failed", t.message ?: "") },
        ) {
            ContainerRuntimeService.getInstance(project).cli.networks.create(name.trim())
            ContainerRuntimeService.getInstance(project).requestRefresh()
        }
    }
}

class DeleteNetworkAction : BaseCliAction() {
    override fun update(e: AnActionEvent) {
        val selection = e.getData(AppleContainerDataKeys.SELECTED_NETWORKS)
        e.presentation.isEnabled = !selection.isNullOrEmpty()
    }

    override fun confirmationMessage(e: AnActionEvent): String {
        val selected = e.getData(AppleContainerDataKeys.SELECTED_NETWORKS).orEmpty()
        return "Delete ${selected.size} network(s)?"
    }

    override suspend fun performCli(project: Project, e: AnActionEvent) {
        val selected = e.getData(AppleContainerDataKeys.SELECTED_NETWORKS) ?: return
        ContainerRuntimeService.getInstance(project).cli.networks.delete(selected.map { it.name })
    }
}

class PruneNetworksAction : BaseCliAction() {
    override fun confirmationMessage(e: AnActionEvent): String = "Remove all unused networks?"
    override suspend fun performCli(project: Project, e: AnActionEvent) {
        ContainerRuntimeService.getInstance(project).cli.networks.prune()
    }
}

class InspectNetworkAction : AnAction("Inspect", "Show full network metadata as JSON", AllIcons.Actions.Preview) {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val selection = e.getData(AppleContainerDataKeys.SELECTED_NETWORKS)
        e.presentation.isEnabled = !selection.isNullOrEmpty()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val selected = e.getData(AppleContainerDataKeys.SELECTED_NETWORKS) ?: return
        PluginScopeService.getInstance(project).launchIo(
            onError = { t -> AppleContainerNotifier.error(project, "Inspect failed", t.message ?: "") },
        ) {
            val json = ContainerRuntimeService.getInstance(project).cli.networks.inspect(selected.map { it.name })
            openJsonPreview(project, "network-inspect.json", json)
        }
    }
}
