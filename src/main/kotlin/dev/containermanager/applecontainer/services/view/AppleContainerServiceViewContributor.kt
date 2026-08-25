package dev.containermanager.applecontainer.services.view

import com.intellij.execution.services.ServiceViewContributor
import com.intellij.execution.services.ServiceViewDescriptor
import com.intellij.icons.AllIcons
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.Project
import javax.swing.JComponent

/**
 * Registered via `<serviceViewContributor implementation="..."/>` in plugin.xml.
 *
 * Shows exactly one entry, "Apple Container", in the Services view — no per-resource tree
 * (no separate node per container/image/volume/...). Clicking it renders the same tabbed panel
 * (Containers/Images/Volumes/Networks/System) that used to live in a standalone Tool Window,
 * reused as-is via [AppleContainerPanelHolder]. The tabs inside that panel are the navigation;
 * the Services tree itself is just the entry point.
 */
class AppleContainerServiceViewContributor : ServiceViewContributor<AppleContainerNode> {

    override fun getViewDescriptor(project: Project): ServiceViewDescriptor = descriptorFor(project)

    override fun getServices(project: Project): List<AppleContainerNode> = listOf(AppleContainerNode(project))

    override fun getServiceDescriptor(project: Project, service: AppleContainerNode): ServiceViewDescriptor =
        descriptorFor(project)

    private fun descriptorFor(project: Project): ServiceViewDescriptor = object : ServiceViewDescriptor {
        override fun getPresentation(): ItemPresentation = object : ItemPresentation {
            override fun getPresentableText() = "Apple Container"
            override fun getIcon(unused: Boolean) = AllIcons.Nodes.Artifact
        }

        override fun getContentComponent(): JComponent = AppleContainerPanelHolder.getInstance(project).panel
    }
}

/** Identity-only marker: there's exactly one "Apple Container" service per project. */
data class AppleContainerNode(val project: Project)