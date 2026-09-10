package dev.containermanager.applecontainer.toolwindow.tabs.containers

import com.intellij.openapi.project.Project
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import dev.containermanager.applecontainer.cli.model.ContainerInfo
import dev.containermanager.applecontainer.toolwindow.table.ContainerTableModelFactory
import java.awt.BorderLayout
import javax.swing.JPanel

/** Renders the live container list with a filterable, sortable table and native controls. */
class ContainersPanel(project: Project) : JPanel(BorderLayout()) {

    private val tableModel = ContainerTableModelFactory.createModel()
    val table: JBTable = ContainerTableModelFactory.createTable(tableModel)
    private var allContainers: List<ContainerInfo> = emptyList()
    private val toolbar = ContainersToolbar(project, ::selectedContainers)

    init {
        val header = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(2)
            add(toolbar, BorderLayout.WEST)
        }

        add(header, BorderLayout.NORTH)
        add(ScrollPaneFactory.createScrollPane(table), BorderLayout.CENTER)

        table.selectionModel.addListSelectionListener {
            if (!it.valueIsAdjusting) toolbar.updateButtons()
        }
    }

    fun update(containers: List<ContainerInfo>) {
        if (allContainers == containers) return

        allContainers = containers
        toolbar.updateButtons()
    }

    fun selectedContainers(): List<ContainerInfo> =
        table.selectedRows.map { table.convertRowIndexToModel(it) }.mapNotNull { tableModel.items.getOrNull(it) }
}
