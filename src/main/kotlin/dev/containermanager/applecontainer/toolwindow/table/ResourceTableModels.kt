package dev.containermanager.applecontainer.toolwindow.table

import com.intellij.ui.table.JBTable
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import dev.containermanager.applecontainer.cli.model.ImageInfo
import dev.containermanager.applecontainer.cli.model.NetworkInfo
import dev.containermanager.applecontainer.cli.model.VolumeInfo

private fun humanBytes(bytes: Long?): String {
    if (bytes == null) return "-"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var i = 0
    while (value >= 1024 && i < units.lastIndex) { value /= 1024; i++ }
    return "%.1f %s".format(value, units[i])
}

private class ImgCol(name: String, private val f: (ImageInfo) -> String) : ColumnInfo<ImageInfo, String>(name) {
    override fun valueOf(item: ImageInfo): String = f(item)
}

object ImageTableModelFactory {
    val columns: Array<ColumnInfo<ImageInfo, *>> = arrayOf(
        ImgCol("Reference") { it.reference },
        ImgCol("Architecture") { it.architecture ?: "-" },
        ImgCol("OS") { it.os ?: "-" },
        ImgCol("Size") { humanBytes(it.sizeBytes) },
        ImgCol("Created") { it.createdAt ?: "-" },
        ImgCol("ID") { it.shortId },
    )

    fun createModel(): ListTableModel<ImageInfo> = ListTableModel(*columns)
    fun createTable(model: ListTableModel<ImageInfo>): JBTable = JBTable(model).apply {
        setShowGrid(false); rowHeight = 26; autoCreateRowSorter = true
    }
}

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
