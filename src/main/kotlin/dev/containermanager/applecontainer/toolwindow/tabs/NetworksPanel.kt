package dev.containermanager.applecontainer.toolwindow.tabs

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import dev.containermanager.applecontainer.actions.AppleContainerDataKeys
import dev.containermanager.applecontainer.cli.model.NetworkInfo
import dev.containermanager.applecontainer.toolwindow.table.NetworkTableModelFactory
import java.awt.BorderLayout
import javax.swing.JPanel

class NetworksPanel(private val project: Project) : JPanel(BorderLayout()), UiDataProvider {

    private val tableModel = NetworkTableModelFactory.createModel()
    val table: JBTable = NetworkTableModelFactory.createTable(tableModel)

    val toolbar = ActionManager.getInstance().createActionToolbar(
        ActionPlaces.TOOLWINDOW_CONTENT,
        ActionManager.getInstance().getAction("AppleContainerManager.NetworksToolbar") as DefaultActionGroup,
        true,
    ).apply { targetComponent = this@NetworksPanel }

    init {

        val header = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(2)
            add(toolbar.component, BorderLayout.WEST)
        }

        add(header, BorderLayout.NORTH)
        add(ScrollPaneFactory.createScrollPane(table), BorderLayout.CENTER)
    }

    fun update(networks: List<NetworkInfo>) {
        if (tableModel.items == networks) return

        tableModel.items = networks
    }

    fun selectedNetworks(): List<NetworkInfo> =
        table.selectedRows.map { table.convertRowIndexToModel(it) }.mapNotNull { tableModel.items.getOrNull(it) }

    override fun uiDataSnapshot(sink: DataSink) {
        sink[AppleContainerDataKeys.SELECTED_NETWORKS] = selectedNetworks()
        sink[PlatformDataKeys.PROJECT] = project
    }
}
