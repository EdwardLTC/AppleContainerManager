package dev.containermanager.applecontainer.toolwindow.tabs.networks

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import dev.containermanager.applecontainer.cli.model.NetworkInfo
import dev.containermanager.applecontainer.services.ContainerRuntimeService
import dev.containermanager.applecontainer.settings.AppleContainerSettingsState
import dev.containermanager.applecontainer.toolwindow.components.ToolbarTaskRunner
import dev.containermanager.applecontainer.toolwindow.components.openJsonPreview
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JPanel

/** Native controls owned by the Networks tab. */
class NetworksToolbar(private val project: Project, private val selectedNetworks: () -> List<NetworkInfo>) :
    JPanel(FlowLayout(FlowLayout.LEFT, 6, 2)) {

    private val runtime = ContainerRuntimeService.getInstance(project)
    private val taskRunner = ToolbarTaskRunner(project)
    private val refreshButton = JButton("Refresh", AllIcons.Actions.Refresh).apply { addActionListener { refreshNetworks() } }
    private val createButton = JButton("Create Network", AllIcons.General.Add).apply { addActionListener { createNetwork() } }
    private val deleteButton = JButton("Delete", AllIcons.Actions.GC).apply { addActionListener { deleteNetworks() } }
    private val inspectButton = JButton("Inspect", AllIcons.Actions.PreviewDetails).apply { addActionListener { inspectNetworks() } }
    private val pruneButton = JButton("Prune", AllIcons.Actions.GC).apply { addActionListener { pruneNetworks() } }

    init {
        listOf(createButton, deleteButton, inspectButton, pruneButton, refreshButton).forEach(::add)
        updateButtons()
    }

    fun updateButtons() {
        if (taskRunner.hasBusyButton(this)) return

        val selection = selectedNetworks()
        createButton.isEnabled = runtime.isServicesRunning()
        if (!taskRunner.isBusy(deleteButton)) deleteButton.isEnabled = selection.isNotEmpty()
        if (!taskRunner.isBusy(inspectButton)) inspectButton.isEnabled = selection.isNotEmpty()
        if (!taskRunner.isBusy(pruneButton)) pruneButton.isEnabled = runtime.isServicesRunning()
    }

    private fun createNetwork() {
        val name = Messages.showInputDialog(project, "Network name:", "Create Network", Messages.getQuestionIcon())?.trim().orEmpty()
        if (name.isNotEmpty()) taskRunner.run(createButton, onFinished = ::updateButtons) { runtime.cli.networks.create(name) }
    }

    private fun refreshNetworks() {
        taskRunner.run(
            refreshButton,
            refreshAfter = false,
            disableWhileRunning = false,
            onFinished = ::updateButtons,
        ) { runtime.refreshNow() }
    }

    private fun deleteNetworks() {
        val networks = selectedNetworks()
        if (networks.isNotEmpty() && confirm("Delete ${networks.size} network(s)?")) {
            taskRunner.run(deleteButton, onFinished = ::updateButtons) { runtime.cli.networks.delete(networks.map { it.name }) }
        }
    }

    private fun inspectNetworks() {
        val networks = selectedNetworks()
        if (networks.isNotEmpty()) taskRunner.run(inspectButton, refreshAfter = false, onFinished = ::updateButtons) {
            openJsonPreview(project, "network-inspect.json", runtime.cli.networks.inspect(networks.map { it.name }))
        }
    }

    private fun pruneNetworks() {
        if (confirm("Remove all unused networks?")) taskRunner.run(pruneButton, onFinished = ::updateButtons) { runtime.cli.networks.prune() }
    }

    private fun confirm(message: String): Boolean =
        !AppleContainerSettingsState.getInstance().confirmDestructiveActions ||
            Messages.showYesNoDialog(project, message, "Confirm Action", Messages.getWarningIcon()) == Messages.YES
}
