package dev.containermanager.applecontainer.toolwindow.table

import com.intellij.ui.ColoredTableCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import dev.containermanager.applecontainer.cli.model.ContainerInfo
import dev.containermanager.applecontainer.cli.model.ContainerStatus
import javax.swing.JTable
import javax.swing.table.TableCellRenderer

private class Col(name: String, private val extractor: (ContainerInfo) -> String) :
    ColumnInfo<ContainerInfo, String>(name) {
    override fun valueOf(item: ContainerInfo): String = extractor(item)
}

private object StatusColumn : ColumnInfo<ContainerInfo, String>("Status") {
    override fun valueOf(item: ContainerInfo): String = item.status.name
    override fun getRenderer(item: ContainerInfo?): TableCellRenderer = StatusCellRenderer
}

private object StatusCellRenderer : ColoredTableCellRenderer() {
    private fun readResolve(): Any = StatusCellRenderer
    override fun customizeCellRenderer(
        table: JTable, value: Any?, selected: Boolean, hasFocus: Boolean, row: Int, column: Int,
    ) {
        val status = (value as? String)?.let { runCatching { ContainerStatus.valueOf(it) }.getOrNull() }
        val (text, attrs) = when (status) {
            ContainerStatus.RUNNING -> "\u25CF Running" to SimpleTextAttributes.REGULAR_ATTRIBUTES.derive(
                SimpleTextAttributes.STYLE_BOLD, java.awt.Color(0x59, 0xA8, 0x69), null, null,
            )
            ContainerStatus.STOPPED, ContainerStatus.EXITED -> "\u25CB Stopped" to SimpleTextAttributes.GRAYED_ATTRIBUTES
            ContainerStatus.CREATED -> "\u25D0 Created" to SimpleTextAttributes.GRAY_ATTRIBUTES
            else -> "? Unknown" to SimpleTextAttributes.GRAY_ATTRIBUTES
        }
        append(text, attrs)
    }
}

object ContainerTableModelFactory {
    val columns: Array<ColumnInfo<ContainerInfo, *>> = arrayOf(
        Col("Name") { it.displayName },
        StatusColumn,
        Col("Image") { it.image },
        Col("Ports") { it.ports.joinToString(", ") { p -> p.toString() } },
        Col("CPUs") { it.cpus?.toString() ?: "-" },
        Col("Memory") { it.memory ?: "-" },
        Col("ID") { it.id.take(12) },
    )

    fun createModel(): ListTableModel<ContainerInfo> = ListTableModel(*columns)

    fun createTable(model: ListTableModel<ContainerInfo>): JBTable = JBTable(model).apply {
        setShowGrid(false)
        rowHeight = 26
        autoCreateRowSorter = true
    }
}
