package dev.containermanager.applecontainer.cli

import java.io.File

/**
 * Resolves the path to the `container` executable.
 *
 * Resolution order:
 *  1. Explicit path from settings (if configured and exists)
 *  2. Common Homebrew / installer locations
 *  3. `PATH` lookup via `which container`
 */
object CliLocator {

    private val commonLocations = listOf(
        "/usr/local/bin/container",
        "/opt/homebrew/bin/container",
        "/usr/bin/container",
    )

    fun resolve(configuredPath: String?): String? {
        if (!configuredPath.isNullOrBlank()) {
            val f = File(configuredPath)
            if (f.exists() && f.canExecute()) return f.absolutePath
        }

        for (candidate in commonLocations) {
            val f = File(candidate)
            if (f.exists() && f.canExecute()) return f.absolutePath
        }

        return findOnPath()
    }

    private fun findOnPath(): String? {
        val pathEnv = System.getenv("PATH") ?: return null
        for (dir in pathEnv.split(File.pathSeparatorChar)) {
            val f = File(dir, "container")
            if (f.exists() && f.canExecute()) return f.absolutePath
        }
        return null
    }
}
