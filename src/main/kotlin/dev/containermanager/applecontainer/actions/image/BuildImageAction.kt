package dev.containermanager.applecontainer.actions.image

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.Messages
import dev.containermanager.applecontainer.cli.model.BuildSpec
import dev.containermanager.applecontainer.services.ContainerRuntimeService
import dev.containermanager.applecontainer.util.ConsoleRunner

class BuildImageAction : AnAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
        val chosen = FileChooser.chooseFile(descriptor, project, project.guessProjectDir())
            ?: return

        val tag = Messages.showInputDialog(
            project, "Tag for the built image:", "Build Image", Messages.getQuestionIcon(),
        ) ?: return
        if (tag.isBlank()) return

        val spec = BuildSpec(contextDir = chosen.path, tags = listOf(tag.trim()))
        val cli = ContainerRuntimeService.getInstance(project).cli
        val handler = cli.images.buildStreaming(spec)
        ConsoleRunner.runInConsole(project, handler, "Build: $tag") {
            ContainerRuntimeService.getInstance(project).requestRefresh()
        }
    }
}