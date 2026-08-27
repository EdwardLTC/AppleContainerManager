package dev.containermanager.applecontainer.toolwindow.tabs

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SearchTextField
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import dev.containermanager.applecontainer.actions.AppleContainerDataKeys
import dev.containermanager.applecontainer.cli.model.ContainerInfo
import dev.containermanager.applecontainer.toolwindow.table.ContainerTableModelFactory
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/** Renders the live container list with a filterable, sortable table and a contextual toolbar. */
class ContainersPanel(private val project: Project) : JPanel(BorderLayout()), UiDataProvider {

    private val tableModel = ContainerTableModelFactory.createModel()
    val table: JBTable = ContainerTableModelFactory.createTable(tableModel)
    private val searchField = SearchTextField()
    private var allContainers: List<ContainerInfo> = emptyList()
    val toolbar = ActionManager.getInstance().createActionToolbar(
        ActionPlaces.TOOLWINDOW_CONTENT,
        ActionManager.getInstance().getAction("AppleContainerManager.ContainersToolbar") as DefaultActionGroup,
        true,
    ).apply { targetComponent = this@ContainersPanel }
    
    init {
        val header = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(2)
            add(toolbar.component, BorderLayout.WEST)
            add(searchField, BorderLayout.EAST)
        }

        add(header, BorderLayout.NORTH)
        add(ScrollPaneFactory.createScrollPane(table), BorderLayout.CENTER)

        searchField.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = applyFilter()
            override fun removeUpdate(e: DocumentEvent) = applyFilter()
            override fun changedUpdate(e: DocumentEvent) = applyFilter()
        })
    }

    fun update(containers: List<ContainerInfo>) {
        if (allContainers == containers) return

        allContainers = containers
        applyFilter()
        toolbar.updateActionsAsync()
    }

    private fun applyFilter() {
        val query = searchField.text.trim().lowercase()
        val filtered = if (query.isEmpty()) {
            allContainers
        } else {
            allContainers.filter {
                it.displayName.lowercase().contains(query) || it.image.lowercase().contains(query)
            }
        }
        val selectedIds = selectedContainers().map { it.id }.toSet()
        tableModel.items = filtered
        if (selectedIds.isNotEmpty()) {
            val indices = filtered.withIndex().filter { it.value.id in selectedIds }.map { it.index }
            indices.forEach { table.addRowSelectionInterval(it, it) }
        }
    }

    fun selectedContainers(): List<ContainerInfo> =
        table.selectedRows.map { table.convertRowIndexToModel(it) }.mapNotNull { tableModel.items.getOrNull(it) }

    override fun uiDataSnapshot(sink: DataSink) {
        sink[AppleContainerDataKeys.SELECTED_CONTAINERS] = selectedContainers()
        sink[PlatformDataKeys.PROJECT] = project
    }
}
