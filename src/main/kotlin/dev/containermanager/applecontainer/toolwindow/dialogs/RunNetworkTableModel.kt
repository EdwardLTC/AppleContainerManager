package dev.containermanager.applecontainer.toolwindow.dialogs

import dev.containermanager.applecontainer.cli.model.NetworkInfo
import javax.swing.table.AbstractTableModel

data class RunNetwork(
    var name: String,
)

class RunNetworkTableModel(networks: List<NetworkInfo>) : AbstractTableModel() {

    private val availableNetworks = networks
    private val items = mutableListOf<RunNetwork>()

    override fun getRowCount(): Int = items.size

    override fun getColumnCount(): Int = 1

    override fun getColumnName(column: Int): String = "Network"

    override fun getValueAt(
        rowIndex: Int,
        columnIndex: Int,
    ): Any? {
        return items.getOrNull(rowIndex)?.name
    }

    override fun isCellEditable(
        rowIndex: Int,
        columnIndex: Int,
    ): Boolean = true

    override fun setValueAt(
        value: Any?,
        rowIndex: Int,
        columnIndex: Int,
    ) {
        if (rowIndex !in items.indices) return

        items[rowIndex].name = when (value) {
            is NetworkInfo -> value.name
            else -> value?.toString().orEmpty()
        }

        fireTableCellUpdated(rowIndex, columnIndex)
    }

    fun addNetwork() {
        val network = availableNetworks.firstOrNull() ?: return

        items += RunNetwork(network.name)

        fireTableRowsInserted(
            items.lastIndex,
            items.lastIndex,
        )
    }

    fun removeNetwork(row: Int) {
        if (row !in items.indices) return

        items.removeAt(row)

        fireTableRowsDeleted(row, row)
    }

    fun networks(): List<String> = items.map { it.name }.filter { it.isNotBlank() }
}