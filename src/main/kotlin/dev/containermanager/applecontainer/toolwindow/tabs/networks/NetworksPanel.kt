package dev.containermanager.applecontainer.toolwindow.tabs.networks

import com.intellij.openapi.project.Project
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import dev.containermanager.applecontainer.cli.model.NetworkInfo
import java.awt.BorderLayout
import javax.swing.JPanel

class NetworksPanel(project: Project) : JPanel(BorderLayout()) {

    private val tableModel = NetworkTableModelFactory.createModel()
    val table: JBTable = NetworkTableModelFactory.createTable(tableModel)

    private val toolbar = NetworksToolbar(project, ::selectedNetworks)

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

    fun update(networks: List<NetworkInfo>) {
        if (tableModel.items != networks) tableModel.items = networks
        toolbar.updateButtons()
    }

    fun selectedNetworks(): List<NetworkInfo> =
        table.selectedRows.map { table.convertRowIndexToModel(it) }.mapNotNull { tableModel.items.getOrNull(it) }
}
