package dev.containermanager.applecontainer.util

fun humanBytes(bytes: Long?): String {
    if (bytes == null) return "-"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var i = 0
    while (value >= 1024 && i < units.lastIndex) {
        value /= 1024; i++
    }
    return "%.1f %s".format(value, units[i])
}
