package dev.containermanager.applecontainer.toolwindow.dialogs

import dev.containermanager.applecontainer.cli.model.VolumeInfo
import javax.swing.table.AbstractTableModel

enum class MountType {
    VOLUME,
    BIND,
}

data class RunMount(
    var type: MountType,
    var source: String,
    var target: String,
    var readOnly: Boolean = false,
)

class RunMountTableModel(volumes: List<VolumeInfo>) : AbstractTableModel() {

    private val items = mutableListOf<RunMount>()

    private val columns = arrayOf(
        "Type",
        "Source",
        "Target",
        "Read-only",
    )

    private val availableVolumes = volumes

    override fun getRowCount(): Int =
        items.size

    override fun getColumnCount(): Int =
        columns.size

    override fun getColumnName(column: Int): String =
        columns[column]

    override fun getValueAt(
        rowIndex: Int,
        columnIndex: Int,
    ): Any? {
        val item = items[rowIndex]

        return when (columnIndex) {
            0 -> item.type
            1 -> item.source
            2 -> item.target
            3 -> item.readOnly
            else -> null
        }
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
        if (rowIndex !in items.indices) {
            return
        }

        val item = items[rowIndex]

        when (columnIndex) {
            0 -> {
                if (value is MountType) {
                    item.type = value
                }
            }

            1 -> {
                item.source = value
                    ?.toString()
                    ?.trim()
                    .orEmpty()
            }

            2 -> {
                item.target = value
                    ?.toString()
                    ?.trim()
                    .orEmpty()
            }

            3 -> {
                item.readOnly = value as? Boolean ?: false
            }
        }

        fireTableCellUpdated(rowIndex, columnIndex)
    }

    fun addMount() {
        items += RunMount(
            type = MountType.VOLUME,
            source = availableVolumes.firstOrNull()?.name.orEmpty(),
            target = "",
            readOnly = false,
        )

        fireTableRowsInserted(
            items.lastIndex,
            items.lastIndex,
        )
    }

    fun removeMount(row: Int) {
        if (row !in items.indices) {
            return
        }

        items.removeAt(row)

        fireTableRowsDeleted(row, row)
    }

    fun item(row: Int): RunMount? =
        items.getOrNull(row)

    fun items(): List<RunMount> =
        items.toList()

    fun toVolumes(): List<String> =
        items
            .filter {
                it.type == MountType.VOLUME &&
                        it.source.isNotBlank() &&
                        it.target.isNotBlank()
            }
            .map {
                buildString {
                    append(it.source)
                    append(":")
                    append(it.target)

                    if (it.readOnly) {
                        append(":ro")
                    }
                }
            }

    fun toMounts(): List<String> =
        items
            .filter {
                it.type == MountType.BIND &&
                        it.source.isNotBlank() &&
                        it.target.isNotBlank()
            }
            .map {
                buildString {
                    append(it.source)
                    append(":")
                    append(it.target)

                    if (it.readOnly) {
                        append(":ro")
                    }
                }
            }
}