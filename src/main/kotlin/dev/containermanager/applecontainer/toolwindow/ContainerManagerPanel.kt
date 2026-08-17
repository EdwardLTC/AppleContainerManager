package dev.containermanager.applecontainer.toolwindow

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTabbedPane
import dev.containermanager.applecontainer.services.ContainerRuntimeService
import dev.containermanager.applecontainer.toolwindow.tabs.ContainersPanel
import dev.containermanager.applecontainer.toolwindow.tabs.ImagesPanel
import dev.containermanager.applecontainer.toolwindow.tabs.NetworksPanel
import dev.containermanager.applecontainer.toolwindow.tabs.SystemPanel
import dev.containermanager.applecontainer.toolwindow.tabs.VolumesPanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import javax.swing.JPanel

/**
 * Hosts the Containers / Images / Volumes / Networks / System tabs and keeps them synced to
 * [ContainerRuntimeService.snapshot]. Subscription runs on a scope tied to this panel's
 * disposal, so the flow collector goes away with the tool window content.
 */
class ContainerManagerPanel(project: Project) : JPanel(BorderLayout()), Disposable {

    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.EDT)

    val containersPanel = ContainersPanel(project)
    val imagesPanel = ImagesPanel(project)
    val volumesPanel = VolumesPanel(project)
    val networksPanel = NetworksPanel(project)
    val systemPanel = SystemPanel(project)

    private val tabs = JBTabbedPane().apply {
        addTab("Containers", containersPanel)
        addTab("Images", imagesPanel)
        addTab("Volumes", volumesPanel)
        addTab("Networks", networksPanel)
        addTab("System", systemPanel)
    }

    init {
        add(tabs, BorderLayout.CENTER)

        val runtime = ContainerRuntimeService.getInstance(project)
        uiScope.launch {
            runtime.snapshot.collectLatest { snapshot ->
                containersPanel.update(snapshot.containers)
                imagesPanel.update(snapshot.images)
                volumesPanel.update(snapshot.volumes)
                networksPanel.update(snapshot.networks)
                systemPanel.onSnapshotUpdate(snapshot.daemonRunning, snapshot.cliAvailable)
            }
        }
    }

    override fun dispose() {
        uiScope.cancel()
    }
}
