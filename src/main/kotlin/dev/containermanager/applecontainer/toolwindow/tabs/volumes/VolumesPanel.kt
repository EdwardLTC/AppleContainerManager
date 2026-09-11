package dev.containermanager.applecontainer.toolwindow.tabs.volumes

import com.intellij.openapi.project.Project
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import dev.containermanager.applecontainer.cli.model.VolumeInfo
import java.awt.BorderLayout
import javax.swing.JPanel

class VolumesPanel(project: Project) : JPanel(BorderLayout()) {

    private val tableModel = VolumeTableModelFactory.createModel()
    val table: JBTable = VolumeTableModelFactory.createTable(tableModel)
    private val toolbar = VolumesToolbar(project, ::selectedVolumes)

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

    fun update(volumes: List<VolumeInfo>) {
        if (tableModel.items != volumes) tableModel.items = volumes
        toolbar.updateButtons()
    }

    fun selectedVolumes(): List<VolumeInfo> =
        table.selectedRows.map { table.convertRowIndexToModel(it) }.mapNotNull { tableModel.items.getOrNull(it) }

}
