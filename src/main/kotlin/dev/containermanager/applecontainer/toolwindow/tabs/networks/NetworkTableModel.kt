package dev.containermanager.applecontainer.toolwindow.tabs.networks

import com.intellij.ui.table.JBTable
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import dev.containermanager.applecontainer.cli.model.NetworkInfo

private class NetCol(name: String, private val f: (NetworkInfo) -> String) : ColumnInfo<NetworkInfo, String>(name) {
    override fun valueOf(item: NetworkInfo): String = f(item)
}

object NetworkTableModelFactory {
    val columns: Array<ColumnInfo<NetworkInfo, *>> = arrayOf(
        NetCol("Name") { it.name },
        NetCol("Plugin") { it.plugin ?: "-" },
        NetCol("Gateway") { it.gateway ?: "-" },
        NetCol("Subnet") { it.subnet ?: "-" },
        NetCol("Subnet v6") { it.subnetV6 ?: "-" },
        NetCol("Mode") { it.mod ?: "-" },
    )

    fun createModel(): ListTableModel<NetworkInfo> = ListTableModel(*columns)
    fun createTable(model: ListTableModel<NetworkInfo>): JBTable = JBTable(model).apply {
        setShowGrid(false); rowHeight = 26; autoCreateRowSorter = true
    }
}
