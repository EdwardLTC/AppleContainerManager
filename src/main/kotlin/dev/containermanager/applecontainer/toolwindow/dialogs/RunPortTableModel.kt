package dev.containermanager.applecontainer.toolwindow.dialogs

import dev.containermanager.applecontainer.cli.model.ExposedPort
import javax.swing.table.AbstractTableModel

data class RunPort(
    var hostPort: Int?,
    var containerPort: Int,
    var protocol: String,
)

class RunPortTableModel(
    exposedPorts: List<ExposedPort>,
) : AbstractTableModel() {

    private val items = exposedPorts.map {
        RunPort(
            hostPort = null,
            containerPort = it.port,
            protocol = it.protocol,
        )
    }.toMutableList()

    private val columns = arrayOf(
        "Host Port",
        "Container Port",
        "Protocol",
    )

    override fun getRowCount(): Int = items.size

    override fun getColumnCount(): Int = columns.size

    override fun getColumnName(column: Int): String =
        columns[column]

    override fun getValueAt(
        rowIndex: Int,
        columnIndex: Int,
    ): Any? {
        val item = items[rowIndex]

        return when (columnIndex) {
            0 -> item.hostPort ?: ""
            1 -> item.containerPort
            2 -> item.protocol
            else -> null
        }
    }

    override fun isCellEditable(
        rowIndex: Int,
        columnIndex: Int,
    ): Boolean {
        return true
    }

    override fun setValueAt(
        value: Any?,
        rowIndex: Int,
        columnIndex: Int,
    ) {
        if (rowIndex !in items.indices) {
            return
        }

        val item = items[rowIndex]

        when (columnIndex) {
            0 -> {
                item.hostPort = value
                    ?.toString()
                    ?.trim()
                    ?.toIntOrNull()
            }

            1 -> {
                item.containerPort = value
                    ?.toString()
                    ?.trim()
                    ?.toIntOrNull()
                    ?: item.containerPort
            }

            2 -> {
                val protocol = value
                    ?.toString()
                    ?.trim()
                    ?.lowercase()

                if (!protocol.isNullOrBlank()) {
                    item.protocol = protocol
                }
            }
        }

        fireTableCellUpdated(rowIndex, columnIndex)
    }

    fun addPort() {
        items.add(
            RunPort(
                hostPort = null,
                containerPort = 0,
                protocol = "tcp",
            )
        )

        fireTableRowsInserted(
            items.lastIndex,
            items.lastIndex,
        )
    }

    fun removePort(row: Int) {
        if (row !in items.indices) {
            return
        }

        items.removeAt(row)

        fireTableRowsDeleted(row, row)
    }

    fun toRunSpecPorts(): List<String> =
        items.mapNotNull { port ->
            val hostPort = port.hostPort ?: return@mapNotNull null

            if (port.containerPort <= 0) {
                return@mapNotNull null
            }

            "$hostPort:${port.containerPort}/${port.protocol}"
        }
}