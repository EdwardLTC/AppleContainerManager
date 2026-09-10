package dev.containermanager.applecontainer.toolwindow.tabs.system

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import dev.containermanager.applecontainer.services.ContainerRuntimeService
import dev.containermanager.applecontainer.toolwindow.components.ToolbarTaskRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JPanel

/** Native controls for starting services and querying the Apple Container system. */
class SystemToolbar(
    private val project: Project,
    private val onStatus: (running: Boolean, raw: String) -> Unit,
    private val onOutput: (String) -> Unit,
) : JPanel(FlowLayout(FlowLayout.LEFT, 6, 2)) {

    private val runtime = ContainerRuntimeService.getInstance(project)
    private val taskRunner = ToolbarTaskRunner(project)
    private var servicesRunning = false
    private val servicesToggleButton = JButton().apply { addActionListener { toggleServices() } }
    private val versionButton = JButton("Version").apply { addActionListener { showVersion() } }
    private val logsButton = JButton("System Logs").apply { addActionListener { showSystemLogs() } }
    private val diskUsageButton = JButton("Disk Usage").apply { addActionListener { showDiskUsage() } }
    private val refreshButton = JButton("Refresh Status", AllIcons.Actions.Refresh).apply { addActionListener { refreshStatus() } }

    init {
        listOf(servicesToggleButton, versionButton, logsButton, diskUsageButton, refreshButton).forEach(::add)
        updateButtons()
        refreshStatus()
    }

    fun onSnapshotUpdate(daemonRunning: Boolean) {
        servicesRunning = daemonRunning
        updateButtons()
    }

    private fun toggleServices() {
        val starting = !servicesRunning
        taskRunner.run(
            servicesToggleButton,
            refreshAfter = false,
            disableWhileRunning = false,
            onFinished = ::updateButtons,
        ) {
            if (starting) runtime.cli.system.start()
            else {
                runtime.cli.system.stop()
                runtime.resetSnapshot()
            }
            val status = runtime.cli.system.status()
            withContext(Dispatchers.EDT) {
                onStatus(status.running, status.raw)
            }
        }
    }

    private fun refreshStatus(): Unit = taskRunner.run(
        refreshButton,
        refreshAfter = false,
        disableWhileRunning = false,
        onFinished = ::updateButtons,
    ) {
        val status = runtime.cli.system.status()
        withContext(Dispatchers.EDT) { onStatus(status.running, status.raw) }
    }

    private fun showVersion(): Unit = taskRunner.run(
        versionButton,
        refreshAfter = false,
        disableWhileRunning = false,
        onFinished = ::updateButtons,
    ) {
        val version = runtime.cli.system.version()
        withContext(Dispatchers.EDT) {
            onOutput(version.components.joinToString("\n") {
                "${it.appName}\t${it.version}\t${it.buildType ?: ""}\t${it.commit ?: ""}"
            })
        }
    }

    private fun showDiskUsage(): Unit = taskRunner.run(
        diskUsageButton,
        refreshAfter = false,
        disableWhileRunning = false,
        onFinished = ::updateButtons,
    ) {
        showOutput(runtime.cli.system.df())
    }

    private fun showSystemLogs(): Unit = taskRunner.run(
        logsButton,
        refreshAfter = false,
        disableWhileRunning = false,
        onFinished = ::updateButtons,
    ) {
        showOutput(runtime.cli.system.logs())
    }

    private suspend fun showOutput(text: String) = withContext(Dispatchers.EDT) { onOutput(text) }

    private fun updateButtons() {
        if (taskRunner.hasBusyButton(this)) return

        if (!taskRunner.isBusy(servicesToggleButton)) {
            servicesToggleButton.text = if (servicesRunning) "Stop Services" else "Start Services"
            servicesToggleButton.icon = if (servicesRunning) AllIcons.Actions.Suspend else AllIcons.Actions.Execute
            servicesToggleButton.isEnabled = true
        }
        if (!taskRunner.isBusy(versionButton)) versionButton.isEnabled = true
        if (!taskRunner.isBusy(logsButton)) logsButton.isEnabled = true
        if (!taskRunner.isBusy(diskUsageButton)) diskUsageButton.isEnabled = servicesRunning
        if (!taskRunner.isBusy(refreshButton)) refreshButton.isEnabled = servicesRunning
    }
}
