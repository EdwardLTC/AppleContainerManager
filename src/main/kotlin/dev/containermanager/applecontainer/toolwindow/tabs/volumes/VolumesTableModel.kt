package dev.containermanager.applecontainer.toolwindow.tabs.volumes

import com.intellij.ui.table.JBTable
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import dev.containermanager.applecontainer.cli.model.VolumeInfo
import dev.containermanager.applecontainer.util.humanBytes

private class VolCol(name: String, private val f: (VolumeInfo) -> String) : ColumnInfo<VolumeInfo, String>(name) {
    override fun valueOf(item: VolumeInfo): String = f(item)
}

object VolumeTableModelFactory {
    val columns: Array<ColumnInfo<VolumeInfo, *>> = arrayOf(
        VolCol("Name") { it.name },
        VolCol("Driver") { it.driver ?: "local" },
        VolCol("Size") { humanBytes(it.sizeBytes) },
        VolCol("Created") { it.createdAt ?: "-" },
    )

    fun createModel(): ListTableModel<VolumeInfo> = ListTableModel(*columns)
    fun createTable(model: ListTableModel<VolumeInfo>): JBTable = JBTable(model).apply {
        setShowGrid(false); rowHeight = 26; autoCreateRowSorter = true
    }
}