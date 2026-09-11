package dev.containermanager.applecontainer.toolwindow.tabs.images

import com.intellij.ui.table.JBTable
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import dev.containermanager.applecontainer.cli.model.ImageInfo
import dev.containermanager.applecontainer.util.humanBytes

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