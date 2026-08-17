package dev.containermanager.applecontainer.toolwindow.tabs

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.actionSystem.UiDataProvider
import com.intellij.openapi.project.Project
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import dev.containermanager.applecontainer.actions.AppleContainerDataKeys
import dev.containermanager.applecontainer.cli.model.VolumeInfo
import dev.containermanager.applecontainer.toolwindow.table.VolumeTableModelFactory
import java.awt.BorderLayout
import javax.swing.JPanel

class VolumesPanel(private val project: Project) : JPanel(BorderLayout()), UiDataProvider {

    private val tableModel = VolumeTableModelFactory.createModel()
    val table: JBTable = VolumeTableModelFactory.createTable(tableModel)

    init {
        val toolbar = ActionManager.getInstance().createActionToolbar(
            ActionPlaces.TOOLWINDOW_CONTENT,
            ActionManager.getInstance().getAction("AppleContainerManager.VolumesToolbar") as DefaultActionGroup,
            true,
        ).apply { targetComponent = this@VolumesPanel }

        val header = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(2)
            add(toolbar.component, BorderLayout.WEST)
        }

        add(header, BorderLayout.NORTH)
        add(ScrollPaneFactory.createScrollPane(table), BorderLayout.CENTER)
    }

    fun update(volumes: List<VolumeInfo>) {
        if (tableModel.items == volumes) return

        tableModel.items = volumes
    }

    fun selectedVolumes(): List<VolumeInfo> =
        table.selectedRows.map { table.convertRowIndexToModel(it) }.mapNotNull { tableModel.items.getOrNull(it) }


    override fun uiDataSnapshot(sink: DataSink) {
        sink[AppleContainerDataKeys.SELECTED_VOLUMES] = selectedVolumes()
        sink[PlatformDataKeys.PROJECT] = project
    }
}
