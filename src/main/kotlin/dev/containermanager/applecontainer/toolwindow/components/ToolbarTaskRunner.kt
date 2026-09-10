package dev.containermanager.applecontainer.toolwindow.components

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.ui.AnimatedIcon
import dev.containermanager.applecontainer.services.ContainerRuntimeService
import dev.containermanager.applecontainer.services.PluginScopeService
import dev.containermanager.applecontainer.util.AppleContainerNotifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Container
import javax.swing.Icon
import javax.swing.JButton

/** Runs a native toolbar command with a busy indicator and optional runtime refresh. */
class ToolbarTaskRunner(private val project: Project) {
    private val runtime = ContainerRuntimeService.getInstance(project)
    private val scopeService = PluginScopeService.getInstance(project)

    fun run(
        button: JButton,
        refreshAfter: Boolean = true,
        disableWhileRunning: Boolean = true,
        disableOtherButtonsWhileRunning: Boolean = true,
        onFinished: () -> Unit,
        block: suspend () -> Unit,
    ) {
        if (isBusy(button)) return

        val idleText = button.text
        val idleIcon = button.icon
        val siblingButtons = button.parent?.components
            ?.filterIsInstance<JButton>()
            ?.filter { it !== button }
            .orEmpty()
        button.putClientProperty(BUSY_KEY, true)
        if (disableWhileRunning) button.isEnabled = false
        if (disableOtherButtonsWhileRunning) siblingButtons.forEach { it.isEnabled = false }
        button.icon = AnimatedIcon.Default.INSTANCE
        button.text = "$idleText…"
        scopeService.launchIo(
            onError = { error ->
                ApplicationManager.getApplication().invokeLater {
                    restore(button, idleText, idleIcon, disableWhileRunning, siblingButtons, onFinished)
                    AppleContainerNotifier.error(project, "Apple Container: $idleText", error.message ?: "Failed")
                }
            },
        ) {
            block()
            if (refreshAfter) runtime.requestRefresh()
            withContext(Dispatchers.EDT) {
                restore(button, idleText, idleIcon, disableWhileRunning, siblingButtons, onFinished)
            }
        }
    }

    fun isBusy(button: JButton): Boolean = button.getClientProperty(BUSY_KEY) == true

    fun hasBusyButton(container: Container): Boolean =
        container.components.filterIsInstance<JButton>().any(::isBusy)

    private fun restore(
        button: JButton,
        text: String?,
        icon: Icon?,
        disableWhileRunning: Boolean,
        siblingButtons: List<JButton>,
        onFinished: () -> Unit,
    ) {
        button.putClientProperty(BUSY_KEY, false)
        button.text = text
        button.icon = icon
        if (disableWhileRunning) button.isEnabled = true
        siblingButtons.forEach { it.isEnabled = true }
        onFinished()
    }

    private companion object {
        const val BUSY_KEY = "busy"
    }
}
