package dev.containermanager.applecontainer.actions.container

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import dev.containermanager.applecontainer.actions.AppleContainerDataKeys
import dev.containermanager.applecontainer.actions.BaseCliAction
import dev.containermanager.applecontainer.cli.model.ContainerStatus
import dev.containermanager.applecontainer.services.ContainerRuntimeService

class RefreshContainersAction : BaseCliAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT
    override suspend fun performCli(project: Project, e: AnActionEvent) {
        // requestRefresh() below (called by the base class after performCli) does the work;
        // nothing additional to invoke here.
    }
}


class ToggleContainerAction : BaseCliAction() {

    override fun getActionUpdateThread() = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        val selected = e.getData(AppleContainerDataKeys.SELECTED_CONTAINERS).orEmpty()
        val presentation = e.presentation

        if (selected.isEmpty()) {
            presentation.isEnabled = false
            presentation.text = "Start"
            presentation.description = "Start the selected container(s)"
            presentation.icon = AllIcons.Actions.Execute
            return
        }

        val allRunning = selected.all { it.status == ContainerStatus.RUNNING }
        val allStopped = selected.none { it.status == ContainerStatus.RUNNING }

        when {
            allRunning -> {
                presentation.isEnabled = true
                presentation.text = "Stop"
                presentation.description = "Stop the selected container(s)"
                presentation.icon = AllIcons.Actions.Suspend
            }

            allStopped -> {
                presentation.isEnabled = true
                presentation.text = "Start"
                presentation.description = "Start the selected container(s)"
                presentation.icon = AllIcons.Actions.Execute
            }

            else -> {
                presentation.isEnabled = false
                presentation.text = "Start / Stop"
                presentation.description = "Selection has mixed states \u2014 select containers with the same status"
                presentation.icon = AllIcons.Actions.Execute
            }
        }
    }

    override fun confirmationMessage(e: AnActionEvent): String? {
        val selected = e.getData(AppleContainerDataKeys.SELECTED_CONTAINERS).orEmpty()
        val allRunning = selected.isNotEmpty() && selected.all { it.status == ContainerStatus.RUNNING }
        return if (allRunning && selected.size > 1) "Stop ${selected.size} containers?" else null
    }

    override suspend fun performCli(project: Project, e: AnActionEvent) {
        val selected = e.getData(AppleContainerDataKeys.SELECTED_CONTAINERS) ?: return
        val cli = ContainerRuntimeService.getInstance(project).cli
        val allRunning = selected.all { it.status == ContainerStatus.RUNNING }
        if (allRunning) {
            cli.containers.stop(selected.map { it.id })
        } else {
            selected.forEach { cli.containers.start(it.id) }
        }
    }
}

class KillContainerAction : BaseCliAction() {
    override fun update(e: AnActionEvent) {
        val selection = e.getData(AppleContainerDataKeys.SELECTED_CONTAINERS) ?: emptyList()
        val allRunning = selection.all { it.status == ContainerStatus.RUNNING }
        e.presentation.isEnabled = selection.isNotEmpty() && allRunning
    }

    override fun confirmationMessage(e: AnActionEvent): String {
        val selected = e.getData(AppleContainerDataKeys.SELECTED_CONTAINERS).orEmpty()
        return "Kill ${selected.size} container(s) immediately? This skips graceful shutdown."
    }

    override suspend fun performCli(project: Project, e: AnActionEvent) {
        val selected = e.getData(AppleContainerDataKeys.SELECTED_CONTAINERS) ?: return
        ContainerRuntimeService.getInstance(project).cli.containers.kill(selected.map { it.id })
    }
}

class DeleteContainerAction : BaseCliAction() {
    override fun update(e: AnActionEvent) {
        val selection = e.getData(AppleContainerDataKeys.SELECTED_CONTAINERS)
        e.presentation.isEnabled = !selection.isNullOrEmpty()
    }

    override fun confirmationMessage(e: AnActionEvent): String {
        val selected = e.getData(AppleContainerDataKeys.SELECTED_CONTAINERS).orEmpty()
        return "Delete ${selected.size} container(s)? Running containers will be force-removed."
    }

    override suspend fun performCli(project: Project, e: AnActionEvent) {
        val selected = e.getData(AppleContainerDataKeys.SELECTED_CONTAINERS) ?: return
        ContainerRuntimeService.getInstance(project).cli.containers.delete(selected.map { it.id }, force = true)
    }
}

class PruneContainersAction : BaseCliAction() {
    override fun confirmationMessage(e: AnActionEvent): String = "Remove all stopped containers?"
    override suspend fun performCli(project: Project, e: AnActionEvent) {
        ContainerRuntimeService.getInstance(project).cli.containers.prune()
    }
}
