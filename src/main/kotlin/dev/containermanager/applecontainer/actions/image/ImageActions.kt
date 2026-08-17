package dev.containermanager.applecontainer.actions.image

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import dev.containermanager.applecontainer.actions.AppleContainerDataKeys
import dev.containermanager.applecontainer.actions.BaseCliAction
import dev.containermanager.applecontainer.actions.container.openJsonPreview
import dev.containermanager.applecontainer.cli.model.RunSpec
import dev.containermanager.applecontainer.services.ContainerRuntimeService
import dev.containermanager.applecontainer.services.PluginScopeService
import dev.containermanager.applecontainer.toolwindow.dialogs.RunImageDialog
import dev.containermanager.applecontainer.util.AppleContainerNotifier
import dev.containermanager.applecontainer.util.ConsoleRunner

class PullImageAction : AnAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val reference = Messages.showInputDialog(
            project, "Image reference to pull:", "Pull Image", Messages.getQuestionIcon(),
        ) ?: return
        if (reference.isBlank()) return

        val cli = ContainerRuntimeService.getInstance(project).cli
        val handler = cli.images.pullStreaming(reference.trim())
        ConsoleRunner.runInConsole(project, handler, "Pull: $reference") {
            ContainerRuntimeService.getInstance(project).requestRefresh()
        }
    }
}

class PushImageAction : AnAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val selection = e.getData(AppleContainerDataKeys.SELECTED_IMAGES)
        e.presentation.isEnabled = selection?.size == 1
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val image = e.getData(AppleContainerDataKeys.SELECTED_IMAGES)?.singleOrNull() ?: return
        val cli = ContainerRuntimeService.getInstance(project).cli
        val handler = cli.images.pushStreaming(image.reference)
        ConsoleRunner.runInConsole(project, handler, "Push: ${image.reference}")
    }
}

class TagImageAction : AnAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val selection = e.getData(AppleContainerDataKeys.SELECTED_IMAGES)
        e.presentation.isEnabled = selection?.size == 1
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val image = e.getData(AppleContainerDataKeys.SELECTED_IMAGES)?.singleOrNull() ?: return
        val newTag = Messages.showInputDialog(
            project, "New tag for ${image.reference}:", "Tag Image", Messages.getQuestionIcon(),
        ) ?: return
        if (newTag.isBlank()) return

        PluginScopeService.getInstance(project).launchIo(
            onError = { t -> AppleContainerNotifier.error(project, "Tag failed", t.message ?: "") },
        ) {
            ContainerRuntimeService.getInstance(project).cli.images.tag(image.reference, newTag.trim())
            ContainerRuntimeService.getInstance(project).requestRefresh()
        }
    }
}

class DeleteImageAction : BaseCliAction() {
    override fun update(e: AnActionEvent) {
        val selection = e.getData(AppleContainerDataKeys.SELECTED_IMAGES)
        e.presentation.isEnabled = !selection.isNullOrEmpty()
    }

    override fun confirmationMessage(e: AnActionEvent): String {
        val selected = e.getData(AppleContainerDataKeys.SELECTED_IMAGES).orEmpty()
        return "Delete ${selected.size} image(s)?"
    }

    override suspend fun performCli(project: Project, e: AnActionEvent) {
        val selected = e.getData(AppleContainerDataKeys.SELECTED_IMAGES) ?: return
        ContainerRuntimeService.getInstance(project).cli.images.delete(selected.map { it.reference }, force = true)
    }
}

class PruneImagesAction : BaseCliAction() {
    override fun confirmationMessage(e: AnActionEvent): String = "Remove all unused images?"
    override suspend fun performCli(project: Project, e: AnActionEvent) {
        ContainerRuntimeService.getInstance(project).cli.images.prune()
    }
}

class InspectImageAction : AnAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val selection = e.getData(AppleContainerDataKeys.SELECTED_IMAGES)
        e.presentation.isEnabled = !selection.isNullOrEmpty()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val selected = e.getData(AppleContainerDataKeys.SELECTED_IMAGES) ?: return
        PluginScopeService.getInstance(project).launchIo(
            onError = { t -> AppleContainerNotifier.error(project, "Inspect failed", t.message ?: "") },
        ) {
            val json = ContainerRuntimeService.getInstance(project).cli.images.inspect(selected.map { it.reference })
            openJsonPreview(project, "image-inspect.json", json)
        }
    }
}

/** Quick-run: launches a foreground container from the selected image with default options. */
class RunImageAction : AnAction() {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val selection = e.getData(AppleContainerDataKeys.SELECTED_IMAGES)

        e.presentation.isEnabled = selection?.size == 1
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val image = e.getData(AppleContainerDataKeys.SELECTED_IMAGES)
            ?.singleOrNull()
            ?: return

        val dialog = RunImageDialog(
            project = project,
            image = image,
            volumes = ContainerRuntimeService.getInstance(project).snapshot.value.volumes,
            networks = ContainerRuntimeService.getInstance(project).snapshot.value.networks
        )

        if (!dialog.showAndGet()) {
            return
        }

        val spec = dialog.runSpec()

        val cli = ContainerRuntimeService
            .getInstance(project)
            .cli

        val handler = cli.containers.runStreaming(spec)

        ConsoleRunner.runInConsole(
            project,
            handler,
            "Run: ${image.reference}",
        ) {
            ContainerRuntimeService.getInstance(project).requestRefresh()
        }
    }
}
