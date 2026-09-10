package dev.containermanager.applecontainer.toolwindow.tabs.system

import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JPanel

/** Displays daemon state and command output; [SystemToolbar] owns the native system controls. */
class SystemPanel(project: Project) : JPanel(BorderLayout()) {

    private val statusLabel = JBLabel("Checking status…")
    private val outputArea = JBTextArea().apply {
        isEditable = false
        lineWrap = false
        border = JBUI.Borders.empty(4)
    }
    private val toolbar = SystemToolbar(
        project = project,
        onStatus = { running, raw ->
            onSnapshotUpdate(running, cliAvailable = true)
            outputArea.text = raw
        },
        onOutput = { outputArea.text = it },
    )

    init {
        val header = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(6, 8, 2, 8)
            add(statusLabel, BorderLayout.WEST)
        }
        add(JPanel(BorderLayout()).apply {
            add(header, BorderLayout.NORTH)
            add(toolbar, BorderLayout.CENTER)
        }, BorderLayout.NORTH)
        add(JBScrollPane(outputArea), BorderLayout.CENTER)
    }

    /** Called by [dev.containermanager.applecontainer.toolwindow.ContainerManagerPanel] on each snapshot tick. */
    fun onSnapshotUpdate(daemonRunning: Boolean, cliAvailable: Boolean) {
        statusLabel.text = when {
            !cliAvailable -> "⚠ `container` CLI not found — check Settings → Apple Container Manager"
            daemonRunning -> "● Container services are running"
            else -> "○ Container services are stopped"
        }
        statusLabel.foreground = if (cliAvailable && daemonRunning) JBColor(0x59A869, 0x59A869) else JBColor.GRAY
        toolbar.onSnapshotUpdate(daemonRunning)
    }
}
