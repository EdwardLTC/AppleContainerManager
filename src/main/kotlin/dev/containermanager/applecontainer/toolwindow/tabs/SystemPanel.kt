package dev.containermanager.applecontainer.toolwindow.tabs

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import dev.containermanager.applecontainer.services.ContainerRuntimeService
import dev.containermanager.applecontainer.services.PluginScopeService
import dev.containermanager.applecontainer.util.AppleContainerNotifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JPanel

/**
 * Shows daemon status/version/disk usage and exposes system + builder lifecycle actions.
 *
 * Start/Stop are collapsed into a single toggle button per lifecycle (services, builder) so the
 * panel reads as "here is the state, here is the one thing you'd do about it" rather than two
 * always-visible buttons where only one is ever meaningful. Every action button shows a spinner
 * and disables itself while its command is in flight.
 */
class SystemPanel(private val project: Project) : JPanel(BorderLayout()) {

    private val statusLabel = JBLabel("Checking status\u2026")
    private val outputArea = JBTextArea().apply {
        isEditable = false
        lineWrap = false
        border = JBUI.Borders.empty(4)
    }

    private val runtime get() = ContainerRuntimeService.getInstance(project)
    private val scopeService get() = PluginScopeService.getInstance(project)

    private var servicesRunning = false

    private val servicesToggleBtn = JButton().apply {
        addActionListener { toggleServices() }
    }
    private val refreshBtn = LoadingButton("Refresh Status", AllIcons.Actions.Refresh) { refreshStatus() }
    private val versionBtn = LoadingButton("Version", null) { showVersion() }
    private val dfBtn = LoadingButton("Disk Usage", null) { showDiskUsage() }
    private val logsBtn = LoadingButton("System Logs", null) { showSystemLogs() }

    init {
        val topRow = JPanel(FlowLayout(FlowLayout.LEFT, 6, 4)).apply {
            add(servicesToggleBtn)
            add(versionBtn)
            add(logsBtn)
            add(dfBtn)
            add(refreshBtn)
        }
        val header = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(6, 8, 2, 8)
            add(statusLabel, BorderLayout.WEST)
        }

        val top = JPanel(BorderLayout()).apply {
            add(header, BorderLayout.NORTH)
            add(JPanel(BorderLayout()).apply {
                add(topRow, BorderLayout.NORTH)
            }, BorderLayout.CENTER)
        }

        add(top, BorderLayout.NORTH)
        add(JBScrollPane(outputArea), BorderLayout.CENTER)

        updateServicesButton()
        refreshStatus()
    }

    /** Called by [dev.containermanager.applecontainer.toolwindow.ContainerManagerPanel] on each snapshot tick. */
    fun onSnapshotUpdate(daemonRunning: Boolean, cliAvailable: Boolean) {
        servicesRunning = daemonRunning
        statusLabel.text = when {
            !cliAvailable -> "\u26A0 `container` CLI not found \u2014 check Settings \u2192 Apple Container Manager"
            daemonRunning -> "\u25CF Container services are running"
            else -> "\u25CB Container services are stopped"
        }
        statusLabel.foreground = if (cliAvailable && daemonRunning) JBColor(0x59A869, 0x59A869) else JBColor.GRAY
        updateServicesButton()
        updateAnotherButton()
    }

    private fun toggleServices() {
        val startingUp = !servicesRunning
        setBusy(servicesToggleBtn, if (startingUp) "Starting\u2026" else "Stopping\u2026")
        runAction(
            onDone = {
                servicesRunning = startingUp
                updateServicesButton()
                refreshStatus()
            },
        ) {
            if (startingUp) runtime.cli.system.start() else runtime.cli.system.stop()
        }
    }

    private fun updateServicesButton() {
        if (servicesToggleBtn.getClientProperty("busy") == true) return
        servicesToggleBtn.text = if (servicesRunning) "Stop Services" else "Start Services"
        servicesToggleBtn.icon = if (servicesRunning) AllIcons.Actions.Suspend else AllIcons.Actions.Execute
        servicesToggleBtn.isEnabled = true
    }

    private fun updateAnotherButton() {
        if (dfBtn.getClientProperty("busy") == true) return
        dfBtn.isEnabled = servicesRunning
        refreshBtn.isEnabled = servicesRunning
    }

    private fun refreshStatus() {
        refreshBtn.runBusy {
            val status = runtime.cli.system.status()
            withContext(Dispatchers.EDT) {
                onSnapshotUpdate(status.running, true)
                outputArea.text = status.raw
            }
        }
    }

    private fun showVersion(): Unit = versionBtn.runBusy {
        val v = runtime.cli.system.version()
        withContext(Dispatchers.EDT) {
            outputArea.text = v.components.joinToString("\n") {
                "${it.appName}\t${it.version}\t${it.buildType ?: ""}\t${it.commit ?: ""}"
            }
        }
    }

    private fun showDiskUsage(): Unit = dfBtn.runBusy { showRaw(runtime.cli.system.df()) }

    private fun showSystemLogs(): Unit = logsBtn.runBusy { showRaw(runtime.cli.system.logs()) }

    private suspend fun showRaw(text: String) = withContext(Dispatchers.EDT) { outputArea.text = text }

    private fun setBusy(button: JButton, busyText: String) {
        button.putClientProperty("busy", true)
        button.isEnabled = false
        button.icon = AnimatedIcon.Default.INSTANCE
        button.text = busyText
    }

    private fun runAction(onDone: () -> Unit, block: suspend () -> Unit) {
        scopeService.launchIo(
            onError = { t ->
                onDoneOnEdt(onDone)
                AppleContainerNotifier.error(project, "Apple Container", t.message ?: "Command failed")
            },
        ) {
            block()
            onDoneOnEdt(onDone)
        }
    }

    private fun onDoneOnEdt(onDone: () -> Unit) {
        servicesToggleBtn.putClientProperty("busy", false)
        onDone()
    }

    /** A button that shows a spinner + disables itself for the duration of its suspend action. */
    private inner class LoadingButton(
        private val idleText: String,
        private val idleIcon: Icon?,
        private val action: suspend () -> Unit,
    ) : JButton(idleText, idleIcon) {
        init {
            addActionListener { runBusy { action() } }
        }

        fun runBusy(block: suspend () -> Unit) {
            icon = AnimatedIcon.Default.INSTANCE
            text = "$idleText\u2026"
            scopeService.launchIo(
                onError = { t ->
                    ApplicationManager.getApplication().invokeLater { resetToIdle() }
                    AppleContainerNotifier.error(project, "Apple Container", t.message ?: "Command failed")
                },
            ) {
                block()
                withContext(Dispatchers.EDT) { resetToIdle() }
            }
        }

        private fun resetToIdle() {
            isEnabled = true
            icon = idleIcon
            text = idleText
        }
    }
}
