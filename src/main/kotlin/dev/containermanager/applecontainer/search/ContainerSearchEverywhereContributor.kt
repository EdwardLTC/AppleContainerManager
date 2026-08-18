package dev.containermanager.applecontainer.search

import com.intellij.icons.AllIcons
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributor
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributorFactory
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.util.Processor
import dev.containermanager.applecontainer.services.ContainerRuntimeService
import javax.swing.JList
import javax.swing.ListCellRenderer

sealed class ContainerSearchItem(open val label: String, open val subtitle: String) {
    data class ContainerItem(override val label: String, override val subtitle: String) :
        ContainerSearchItem(label, subtitle)
    data class ImageItem(override val label: String, override val subtitle: String) :
        ContainerSearchItem(label, subtitle)
}

/**
 * Lets developers jump straight to a running container or a pulled image from Search
 * Everywhere (double-shift) instead of having to open the tool window and scan the table.
 * Selecting an item simply focuses the Apple Container tool window \u2014 the CLI has no
 * "open file" analog, so this acts as fast navigation rather than a file locator.
 */
class ContainerSearchEverywhereContributor(private val project: Project) :
    SearchEverywhereContributor<ContainerSearchItem> {

    override fun getSearchProviderId(): String = "AppleContainerManager.SearchEverywhere"
    override fun getGroupName(): String = "Apple Containers"
    override fun getSortWeight(): Int = 500
    override fun showInFindResults(): Boolean = false
    override fun isShownInSeparateTab(): Boolean = false

    override fun fetchElements(
        pattern: String,
        progressIndicator: com.intellij.openapi.progress.ProgressIndicator,
        consumer: Processor<in ContainerSearchItem>,
    ) {
        val snapshot = ContainerRuntimeService.getInstance(project).snapshot.value
        val query = pattern.trim().lowercase()

        snapshot.containers
            .filter { query.isEmpty() || it.displayName.lowercase().contains(query) }
            .forEach { consumer.process(ContainerSearchItem.ContainerItem(it.displayName, it.image)) }

        snapshot.images
            .filter { query.isEmpty() || it.reference.lowercase().contains(query) }
            .forEach { consumer.process(ContainerSearchItem.ImageItem(it.reference, it.shortId)) }
    }

    override fun processSelectedItem(selected: ContainerSearchItem, modifiers: Int, searchText: String): Boolean {
        ToolWindowManager.getInstance(project).getToolWindow("Apple Container")?.activate(null)
        return true
    }

    override fun getElementsRenderer(): ListCellRenderer<in ContainerSearchItem> =
        ListCellRenderer { list: JList<out ContainerSearchItem>, value, _, isSelected, _ ->
            val icon = when (value) {
                is ContainerSearchItem.ContainerItem -> AllIcons.RunConfigurations.Application
                is ContainerSearchItem.ImageItem -> AllIcons.Actions.Download
            }
            com.intellij.ui.components.JBLabel("${value.label}  \u2014  ${value.subtitle}", icon, javax.swing.SwingConstants.LEFT).apply {
                isOpaque = true
                background = if (isSelected) list.selectionBackground else list.background
                foreground = if (isSelected) list.selectionForeground else list.foreground
                border = com.intellij.util.ui.JBUI.Borders.empty(2, 8)
            }
        }

    class Factory : SearchEverywhereContributorFactory<ContainerSearchItem> {
        override fun createContributor(event: AnActionEvent): SearchEverywhereContributor<ContainerSearchItem> {
            val project = requireNotNull(event.project)
            return ContainerSearchEverywhereContributor(project)
        }
    }
}
