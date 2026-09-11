package dev.containermanager.applecontainer.toolwindow.tabs.images

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.isFile
import dev.containermanager.applecontainer.cli.model.BuildSpec
import dev.containermanager.applecontainer.cli.model.ImageInfo
import dev.containermanager.applecontainer.services.ContainerRuntimeService
import dev.containermanager.applecontainer.settings.AppleContainerSettingsState
import dev.containermanager.applecontainer.toolwindow.components.ToolbarTaskRunner
import dev.containermanager.applecontainer.toolwindow.components.openJsonPreview
import dev.containermanager.applecontainer.toolwindow.dialogs.RunImageDialog
import dev.containermanager.applecontainer.util.ConsoleRunner
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JPanel

/** Native controls owned by the Images tab. */
class ImagesToolbar(private val project: Project, private val selectedImages: () -> List<ImageInfo>) :
    JPanel(FlowLayout(FlowLayout.LEFT, 6, 2)) {

    private val runtime = ContainerRuntimeService.getInstance(project)
    private val taskRunner = ToolbarTaskRunner(project)
    private val refreshButton =
        JButton("Refresh", AllIcons.Actions.Refresh).apply { addActionListener { refreshImages() } }
    private val pullButton =
        JButton("Pull Image…", AllIcons.Actions.Download).apply { addActionListener { pullImage() } }
    private val buildButton =
        JButton("Build Image", AllIcons.Actions.Compile).apply { addActionListener { buildImage() } }
    private val runButton = JButton("Run Image", AllIcons.Actions.Execute).apply { addActionListener { runImage() } }
    private val pushButton = JButton("Push Image", AllIcons.Actions.Upload).apply { addActionListener { pushImage() } }
    private val tagButton = JButton("Tag").apply { addActionListener { tagImage() } }
    private val deleteButton = JButton("Delete", AllIcons.Actions.GC).apply { addActionListener { deleteImages() } }
    private val inspectButton =
        JButton("Inspect", AllIcons.Actions.Preview).apply { addActionListener { inspectImages() } }
    private val pruneButton = JButton("Prune", AllIcons.Actions.ClearCash).apply { addActionListener { pruneImages() } }

    init {
        listOf(
            runButton,
            tagButton,
            deleteButton,
            inspectButton,
            pullButton,
            buildButton,
            pushButton,
            pruneButton,
            refreshButton
        ).forEach(::add)
        updateButtons()
    }

    fun updateButtons() {
        if (taskRunner.hasBusyButton(this)) return

        val selection = selectedImages()
        val online = runtime.isServicesRunning()
        pullButton.isEnabled = online
        buildButton.isEnabled = online
        runButton.isEnabled = selection.size == 1
        pushButton.isEnabled = selection.size == 1
        tagButton.isEnabled = selection.size == 1
        if (!taskRunner.isBusy(deleteButton)) deleteButton.isEnabled = selection.isNotEmpty()
        if (!taskRunner.isBusy(inspectButton)) inspectButton.isEnabled = selection.isNotEmpty()
        if (!taskRunner.isBusy(pruneButton)) pruneButton.isEnabled = online
    }

    private fun pullImage() {
        val reference =
            Messages.showInputDialog(project, "Image reference to pull:", "Pull Image", Messages.getQuestionIcon())
                ?.trim().orEmpty()
        if (reference.isNotEmpty()) ConsoleRunner.runInConsole(
            project,
            runtime.cli.images.pullStreaming(reference),
            "Pull: $reference"
        ) { runtime.requestRefresh() }
    }

    private fun refreshImages() {
        taskRunner.run(
            refreshButton,
            refreshAfter = false,
            disableWhileRunning = false,
            onFinished = ::updateButtons,
        ) { runtime.refreshNow() }
    }

    private fun buildImage() {
        val descriptor = FileChooserDescriptor(true, false, false, false, false, false).apply {
            title = "Select Dockerfile"
            withFileFilter { file -> file.isFile && (file.name == "Dockerfile" || file.name.startsWith("Dockerfile.")) }
        }
        val dockerfile = FileChooser.chooseFile(descriptor, project, project.guessProjectDir()) ?: return
        val tag =
            Messages.showInputDialog(project, "Tag for the built image:", "Build Image", Messages.getQuestionIcon())
                ?.trim().orEmpty()
        if (tag.isNotEmpty()) {
            val spec = BuildSpec(dockerfile.parent.path, dockerfile.path, listOf(tag))
            ConsoleRunner.runInConsole(
                project,
                runtime.cli.images.buildStreaming(spec),
                "Build: $tag"
            ) { runtime.requestRefresh() }
        }
    }

    private fun runImage() {
        val image = selectedImages().singleOrNull() ?: return
        val dialog = RunImageDialog(project, image, runtime.snapshot.value.volumes, runtime.snapshot.value.networks)
        if (dialog.showAndGet()) ConsoleRunner.runInConsole(
            project,
            runtime.cli.containers.runStreaming(dialog.runSpec()),
            "Run: ${image.reference}"
        ) { runtime.requestRefresh() }
    }

    private fun pushImage() {
        val image = selectedImages().singleOrNull() ?: return
        ConsoleRunner.runInConsole(
            project,
            runtime.cli.images.pushStreaming(image.reference),
            "Push: ${image.reference}"
        )
    }

    private fun tagImage() {
        val image = selectedImages().singleOrNull() ?: return
        val tag = Messages.showInputDialog(
            project,
            "New tag for ${image.reference}:",
            "Tag Image",
            Messages.getQuestionIcon()
        )?.trim().orEmpty()
        if (tag.isNotEmpty()) taskRunner.run(
            tagButton,
            onFinished = ::updateButtons
        ) { runtime.cli.images.tag(image.reference, tag) }
    }

    private fun deleteImages() {
        val images = selectedImages()
        if (images.isNotEmpty() && confirm("Delete ${images.size} image(s)?")) {
            taskRunner.run(
                deleteButton,
                onFinished = ::updateButtons
            ) { runtime.cli.images.delete(images.map { it.reference }, force = true) }
        }
    }

    private fun inspectImages() {
        val images = selectedImages()
        if (images.isNotEmpty()) taskRunner.run(inspectButton, refreshAfter = false, onFinished = ::updateButtons) {
            openJsonPreview(project, "image-inspect.json", runtime.cli.images.inspect(images.map { it.reference }))
        }
    }

    private fun pruneImages() {
        if (confirm("Remove all unused images?")) taskRunner.run(
            pruneButton,
            onFinished = ::updateButtons
        ) { runtime.cli.images.prune(true) }
    }

    private fun confirm(message: String): Boolean =
        !AppleContainerSettingsState.getInstance().confirmDestructiveActions ||
                Messages.showYesNoDialog(project, message, "Confirm Action", Messages.getWarningIcon()) == Messages.YES
}
