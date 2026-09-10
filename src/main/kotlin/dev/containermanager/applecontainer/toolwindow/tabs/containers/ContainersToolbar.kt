package dev.containermanager.applecontainer.toolwindow.tabs.containers

import com.intellij.icons.AllIcons
import com.intellij.ide.setToolTipText
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.text.HtmlChunk
import dev.containermanager.applecontainer.cli.model.ContainerInfo
import dev.containermanager.applecontainer.cli.model.ContainerStatus
import dev.containermanager.applecontainer.services.ContainerRuntimeService
import dev.containermanager.applecontainer.settings.AppleContainerSettingsState
import dev.containermanager.applecontainer.toolwindow.components.ToolbarTaskRunner
import dev.containermanager.applecontainer.toolwindow.components.openJsonPreview
import dev.containermanager.applecontainer.util.ConsoleRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JPanel

/**
 * Native controls for the Containers tab.
 *
 * Unlike an IDE [com.intellij.openapi.actionSystem.ActionToolbar], the button state is refreshed
 * directly when the table selection or runtime snapshot changes.
 */
class ContainersToolbar(
    private val project: Project,
    private val selectedContainers: () -> List<ContainerInfo>,
) : JPanel(FlowLayout(FlowLayout.LEFT, 6, 2)) {

    private val runtime = ContainerRuntimeService.getInstance(project)
    private val taskRunner = ToolbarTaskRunner(project)

    private val refreshButton = JButton("Refresh", AllIcons.Actions.Refresh).apply {
        addActionListener { refreshContainers() }
    }
    private val toggleButton = JButton().apply { addActionListener { toggleContainers() } }
    private val killButton = JButton("Kill", AllIcons.Process.Stop).apply { addActionListener { killContainers() } }
    private val deleteButton = JButton("Delete", AllIcons.Actions.GC).apply { addActionListener { deleteContainers() } }
    private val logsButton =
        JButton("View Logs", AllIcons.Actions.ShowAsTree).apply { addActionListener { viewLogs() } }
    private val execButton =
        JButton("Exec Shell…", AllIcons.Debugger.Console).apply { addActionListener { execInContainer() } }
    private val statsButton =
        JButton("View Live Stats", AllIcons.Actions.Profile).apply { addActionListener { viewStats() } }
    private val inspectButton =
        JButton("Inspect", AllIcons.Actions.Preview).apply { addActionListener { inspectContainers() } }
    private val pruneButton =
        JButton("Prune", AllIcons.Actions.ClearCash).apply { addActionListener { pruneContainers() } }

    init {
        listOf(
            toggleButton,
            killButton,
            deleteButton,
            logsButton,
            execButton,
            statsButton,
            inspectButton,
            pruneButton,
            refreshButton,
        ).forEach(::add)
        updateButtons()
    }

    fun updateButtons() {
        if (taskRunner.hasBusyButton(this)) return

        val selection = selectedContainers()
        val allRunning = selection.isNotEmpty() && selection.all { it.status == ContainerStatus.RUNNING }
        val allStopped = selection.isNotEmpty() && selection.none { it.status == ContainerStatus.RUNNING }

        toggleButton.apply {
            if (taskRunner.isBusy(this)) return@apply
            isEnabled = allRunning || allStopped
            text = when {
                allRunning -> "Stop"
                allStopped -> "Start"
                else -> "Start / Stop"
            }
            icon = if (allRunning) AllIcons.Actions.Suspend else AllIcons.Actions.Execute
            setToolTipText(
                if (selection.isNotEmpty() && !allRunning && !allStopped) {
                    HtmlChunk.text("Selection has mixed states — select containers with the same status")
                } else {
                    HtmlChunk.text("Start or stop the selected container(s)")
                }
            )
        }
        if (!taskRunner.isBusy(killButton)) killButton.isEnabled = selection.isNotEmpty() && allRunning
        if (!taskRunner.isBusy(deleteButton)) deleteButton.isEnabled = selection.isNotEmpty()
        logsButton.isEnabled = selection.size == 1
        execButton.isEnabled = selection.size == 1
        statsButton.isEnabled = selection.isNotEmpty()
        pruneButton.isEnabled = runtime.isServicesRunning()
        if (!taskRunner.isBusy(inspectButton)) inspectButton.isEnabled = selection.isNotEmpty()
    }

    private fun toggleContainers() {
        val selected = selectedContainers()
        val allRunning = selected.isNotEmpty() && selected.all { it.status == ContainerStatus.RUNNING }
        if (allRunning && selected.size > 1 && !confirm("Stop ${selected.size} containers?")) return

        runCli(toggleButton) {
            if (allRunning) runtime.cli.containers.stop(selected.map { it.id })
            else selected.forEach { runtime.cli.containers.start(it.id) }
        }
    }

    private fun refreshContainers() {
        taskRunner.run(
            refreshButton,
            refreshAfter = false,
            disableWhileRunning = false,
            onFinished = ::updateButtons,
        ) { runtime.refreshNow() }
    }

    private fun killContainers() {
        val selected = selectedContainers()
        if (!confirm("Kill ${selected.size} container(s) immediately? This skips graceful shutdown.")) return
        runCli(killButton) { runtime.cli.containers.kill(selected.map { it.id }) }
    }

    private fun deleteContainers() {
        val selected = selectedContainers()
        if (!confirm("Delete ${selected.size} container(s)? Running containers will be force-removed.")) return
        runCli(deleteButton) { runtime.cli.containers.delete(selected.map { it.id }, force = true) }
    }

    private fun pruneContainers() {
        if (!confirm("Remove all stopped containers?")) return
        runCli(pruneButton) { runtime.cli.containers.prune() }
    }

    private fun viewLogs() {
        val container = selectedContainers().singleOrNull() ?: return
        ConsoleRunner.runInConsole(
            project,
            runtime.cli.containers.logsStreaming(container.id, follow = true),
            "Logs: ${container.displayName}"
        )
    }

    private fun execInContainer() {
        val container = selectedContainers().singleOrNull() ?: return
        val command = Messages.showInputDialog(
            project,
            "Command to execute inside ${container.displayName}:",
            "Exec in Container",
            Messages.getQuestionIcon(),
            "/bin/sh",
            null,
        ) ?: return
        ConsoleRunner.runInConsole(
            project,
            runtime.cli.containers.execStreaming(container.id, listOf(command), interactive = true, tty = true),
            "Exec: ${container.displayName}",
        )
    }

    private fun viewStats() {
        val selected = selectedContainers()
        if (selected.isNotEmpty()) ConsoleRunner.runInConsole(
            project,
            runtime.cli.containers.statsStreaming(selected.map { it.id }),
            "Stats"
        )
    }

    private fun inspectContainers() {
        val selected = selectedContainers()
        if (selected.isEmpty()) return
        runCli(inspectButton, refreshAfter = false) {
            val json = runtime.cli.containers.inspect(selected.map { it.id })
            withContext(Dispatchers.EDT) {
                openJsonPreview(project, "container-inspect-${selected.first().id.take(8)}.json", json)
            }
        }
    }

    private fun confirm(message: String): Boolean =
        !AppleContainerSettingsState.getInstance().confirmDestructiveActions ||
                Messages.showYesNoDialog(project, message, "Confirm Action", Messages.getWarningIcon()) == Messages.YES

    private fun runCli(button: JButton, refreshAfter: Boolean = true, block: suspend () -> Unit) {
        taskRunner.run(button, refreshAfter = refreshAfter, onFinished = ::updateButtons, block = block)
    }
}
