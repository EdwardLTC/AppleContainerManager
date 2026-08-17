package dev.containermanager.applecontainer.listeners

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import dev.containermanager.applecontainer.services.ContainerRuntimeService
import dev.containermanager.applecontainer.util.AppleContainerNotifier

/** Runs once per project open; nudges the user toward Settings if the CLI isn't found. */
class AppleContainerStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val cli = ContainerRuntimeService.getInstance(project).cli
        if (!cli.isCliAvailable()) {
            AppleContainerNotifier.warn(
                project,
                "Apple Container CLI not found",
                "Install it from github.com/apple/container, or set its path in " +
                    "Settings \u2192 Tools \u2192 Apple Container Manager.",
            )
        }
    }
}
