package dev.containermanager.applecontainer.toolwindow.tabs.images

import com.intellij.openapi.project.Project
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import dev.containermanager.applecontainer.cli.model.ImageInfo
import java.awt.BorderLayout
import javax.swing.JPanel

class ImagesPanel(project: Project) : JPanel(BorderLayout()) {

    private val tableModel = ImageTableModelFactory.createModel()
    val table: JBTable = ImageTableModelFactory.createTable(tableModel)
    private val toolbar = ImagesToolbar(project, ::selectedImages)

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

    fun update(images: List<ImageInfo>) {
        if (tableModel.items != images) tableModel.items = images
        toolbar.updateButtons()
    }

    fun selectedImages(): List<ImageInfo> =
        table.selectedRows.map { table.convertRowIndexToModel(it) }.mapNotNull { tableModel.items.getOrNull(it) }
}
