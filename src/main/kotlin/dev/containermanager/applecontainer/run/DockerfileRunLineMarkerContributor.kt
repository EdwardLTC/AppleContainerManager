package dev.containermanager.applecontainer.run

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiElement
import dev.containermanager.applecontainer.cli.model.BuildSpec
import dev.containermanager.applecontainer.services.ContainerRuntimeService
import dev.containermanager.applecontainer.util.ConsoleRunner

/**
 * Adds a gutter run icon on the first line of any `Dockerfile` / `Containerfile`, so building an
 * image feels like running any other file in the IDE rather than requiring a trip to the tool
 * window. Scoped to plain-text files (the default file type for Dockerfiles when no dedicated
 * Docker language plugin is installed) and filtered by filename.
 */
class DockerfileRunLineMarkerContributor : RunLineMarkerContributor() {

    override fun getInfo(element: PsiElement): Info? {
        if (element.textOffset != 0) return null
        val file = element.containingFile?.virtualFile ?: return null
        if (!isContainerBuildFile(file.name)) return null

        return Info(
            AllIcons.Actions.Compile,
            { "Build with Apple Container" },
            BuildFromDockerfileAction(file.path),
        )
    }

    private fun isContainerBuildFile(name: String): Boolean =
        name == "Dockerfile" || name == "Containerfile" ||
            name.endsWith(".dockerfile", ignoreCase = true) ||
            name.startsWith("Dockerfile.")
}

private class BuildFromDockerfileAction(private val dockerfilePath: String) :
    AnAction("Build with Apple Container", "Build an image from this Dockerfile", AllIcons.Actions.Compile) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val contextDir = java.io.File(dockerfilePath).parentFile?.path ?: return

        val tag = Messages.showInputDialog(
            project, "Tag for the built image:", "Build Image", Messages.getQuestionIcon(),
        ) ?: return
        if (tag.isBlank()) return

        val spec = BuildSpec(contextDir = contextDir, dockerfile = dockerfilePath, tags = listOf(tag.trim()))
        val cli = ContainerRuntimeService.getInstance(project).cli
        val handler = cli.images.buildStreaming(spec)
        ConsoleRunner.runInConsole(project, handler, "Build: $tag") {
            ContainerRuntimeService.getInstance(project).requestRefresh()
        }
    }
}
