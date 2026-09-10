package dev.containermanager.applecontainer.toolwindow.tabs.volumes

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import dev.containermanager.applecontainer.cli.model.VolumeInfo
import dev.containermanager.applecontainer.services.ContainerRuntimeService
import dev.containermanager.applecontainer.settings.AppleContainerSettingsState
import dev.containermanager.applecontainer.toolwindow.components.ToolbarTaskRunner
import dev.containermanager.applecontainer.toolwindow.components.openJsonPreview
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JPanel

/** Native controls owned by the Volumes tab. */
class VolumesToolbar(private val project: Project, private val selectedVolumes: () -> List<VolumeInfo>) :
    JPanel(FlowLayout(FlowLayout.LEFT, 6, 2)) {

    private val runtime = ContainerRuntimeService.getInstance(project)
    private val taskRunner = ToolbarTaskRunner(project)
    private val refreshButton = JButton("Refresh", AllIcons.Actions.Refresh).apply { addActionListener { refreshVolumes() } }
    private val createButton = JButton("Create Volume", AllIcons.General.Add).apply { addActionListener { createVolume() } }
    private val deleteButton = JButton("Delete", AllIcons.Actions.GC).apply { addActionListener { deleteVolumes() } }
    private val inspectButton = JButton("Inspect", AllIcons.Actions.PreviewDetails).apply { addActionListener { inspectVolumes() } }
    private val pruneButton = JButton("Prune", AllIcons.Actions.GC).apply { addActionListener { pruneVolumes() } }

    init {
        listOf(createButton, deleteButton, inspectButton, pruneButton, refreshButton).forEach(::add)
        updateButtons()
    }

    fun updateButtons() {
        if (taskRunner.hasBusyButton(this)) return

        val selection = selectedVolumes()
        createButton.isEnabled = runtime.isServicesRunning()
        if (!taskRunner.isBusy(deleteButton)) deleteButton.isEnabled = selection.isNotEmpty()
        if (!taskRunner.isBusy(inspectButton)) inspectButton.isEnabled = selection.isNotEmpty()
        if (!taskRunner.isBusy(pruneButton)) pruneButton.isEnabled = runtime.isServicesRunning()
    }

    private fun createVolume() {
        val name = Messages.showInputDialog(project, "Volume name:", "Create Volume", Messages.getQuestionIcon())?.trim().orEmpty()
        if (name.isNotEmpty()) taskRunner.run(createButton, onFinished = ::updateButtons) { runtime.cli.volumes.create(name) }
    }

    private fun refreshVolumes() {
        taskRunner.run(
            refreshButton,
            refreshAfter = false,
            disableWhileRunning = false,
            onFinished = ::updateButtons,
        ) { runtime.refreshNow() }
    }

    private fun deleteVolumes() {
        val volumes = selectedVolumes()
        if (volumes.isNotEmpty() && confirm("Delete ${volumes.size} volume(s)? Data will be permanently lost.")) {
            taskRunner.run(deleteButton, onFinished = ::updateButtons) { runtime.cli.volumes.delete(volumes.map { it.name }) }
        }
    }

    private fun inspectVolumes() {
        val volumes = selectedVolumes()
        if (volumes.isNotEmpty()) taskRunner.run(inspectButton, refreshAfter = false, onFinished = ::updateButtons) {
            openJsonPreview(project, "volume-inspect.json", runtime.cli.volumes.inspect(volumes.map { it.name }))
        }
    }

    private fun pruneVolumes() {
        if (confirm("Remove all volumes not referenced by any container? Data will be permanently lost.")) {
            taskRunner.run(pruneButton, onFinished = ::updateButtons) { runtime.cli.volumes.prune() }
        }
    }

    private fun confirm(message: String): Boolean =
        !AppleContainerSettingsState.getInstance().confirmDestructiveActions ||
            Messages.showYesNoDialog(project, message, "Confirm Action", Messages.getWarningIcon()) == Messages.YES
}
