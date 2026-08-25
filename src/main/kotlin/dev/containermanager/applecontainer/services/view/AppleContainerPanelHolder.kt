package dev.containermanager.applecontainer.services.view

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import dev.containermanager.applecontainer.toolwindow.ContainerManagerPanel

/**
 * Holds the single [ContainerManagerPanel] instance shown by the "Apple Container" Services
 * node. Needed because [AppleContainerServiceViewContributor] may be asked for its content
 * component more than once (once for the contributor's own view descriptor, once for the single
 * service item's descriptor) — without this, each call would build a brand-new panel with its
 * own coroutine subscription to [dev.containermanager.applecontainer.services.ContainerRuntimeService],
 * duplicating work and potentially showing stale/inconsistent tab state.
 *
 * As a project-level [Disposable] service, the platform disposes it (and therefore the panel,
 * which cancels its own EDT-bound coroutine scope) automatically when the project closes.
 */
@Service(Service.Level.PROJECT)
class AppleContainerPanelHolder(private val project: Project) : Disposable {

    val panel: ContainerManagerPanel by lazy { ContainerManagerPanel(project) }

    override fun dispose() {
        Disposer.dispose(panel)
    }

    companion object {
        fun getInstance(project: Project): AppleContainerPanelHolder =
            project.getService(AppleContainerPanelHolder::class.java)
    }
}