package dev.containermanager.applecontainer.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import dev.containermanager.applecontainer.services.ContainerRuntimeService
import dev.containermanager.applecontainer.services.PluginScopeService
import dev.containermanager.applecontainer.settings.AppleContainerSettingsState
import dev.containermanager.applecontainer.util.AppleContainerNotifier

/**
 * Common scaffolding for actions that invoke a suspend CLI call: EDT confirmation for
 * destructive operations, background execution via [PluginScopeService], and a refresh +
 * notification once done. Subclasses just implement [performCli] and describe themselves.
 */
abstract class BaseCliAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    /** Return a confirmation message to show before running, or null to skip confirmation. */
    open fun confirmationMessage(e: AnActionEvent): String? = null

    abstract suspend fun performCli(project: Project, e: AnActionEvent)

    open fun successMessage(e: AnActionEvent): String? = null

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val message = confirmationMessage(e)
        if (message != null && AppleContainerSettingsState.getInstance().confirmDestructiveActions) {
            val result = Messages.showYesNoDialog(
                project, message, templateText ?: "Confirm Action", Messages.getWarningIcon(),
            )
            if (result != Messages.YES) return
        }

        PluginScopeService.getInstance(project).launchIo(
            onError = { t ->
                AppleContainerNotifier.error(project, "Apple Container: ${templateText}", t.message ?: "Failed")
            },
        ) {
            performCli(project, e)
            successMessage(e)?.let { AppleContainerNotifier.info(project, it) }
            ContainerRuntimeService.getInstance(project).requestRefresh()
        }
    }
}
