package dev.containermanager.applecontainer.actions.container

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import dev.containermanager.applecontainer.actions.AppleContainerDataKeys
import dev.containermanager.applecontainer.actions.BaseCliAction
import dev.containermanager.applecontainer.services.ContainerRuntimeService

class RefreshContainersAction : BaseCliAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT
    override suspend fun performCli(project: Project, e: AnActionEvent) {
        // requestRefresh() below (called by the base class after performCli) does the work;
        // nothing additional to invoke here.
    }
}

class StartContainerAction : BaseCliAction() {
    override fun update(e: AnActionEvent) {
        val selection = e.getData(AppleContainerDataKeys.SELECTED_CONTAINERS)
        e.presentation.isEnabled = !selection.isNullOrEmpty()
    }

    override suspend fun performCli(project: Project, e: AnActionEvent) {
        val selected = e.getData(AppleContainerDataKeys.SELECTED_CONTAINERS) ?: return
        val cli = ContainerRuntimeService.getInstance(project).cli
        selected.forEach { cli.containers.start(it.id) }
    }
}

class StopContainerAction : BaseCliAction() {
    override fun update(e: AnActionEvent) {
        val selection = e.getData(AppleContainerDataKeys.SELECTED_CONTAINERS)
        e.presentation.isEnabled = !selection.isNullOrEmpty()
    }

    override fun confirmationMessage(e: AnActionEvent): String? {
        val selected = e.getData(AppleContainerDataKeys.SELECTED_CONTAINERS) ?: return null
        if (selected.size <= 1) return null
        return "Stop ${selected.size} containers?"
    }

    override suspend fun performCli(project: Project, e: AnActionEvent) {
        val selected = e.getData(AppleContainerDataKeys.SELECTED_CONTAINERS) ?: return
        ContainerRuntimeService.getInstance(project).cli.containers.stop(selected.map { it.id })
    }
}

class KillContainerAction : BaseCliAction() {
    override fun update(e: AnActionEvent) {
        val selection = e.getData(AppleContainerDataKeys.SELECTED_CONTAINERS)
        e.presentation.isEnabled = !selection.isNullOrEmpty()
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
