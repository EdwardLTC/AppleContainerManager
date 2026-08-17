package dev.containermanager.applecontainer.run

import com.intellij.openapi.options.SettingsEditor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

/**
 * Kotlin UI DSL editor for [ContainerRunConfiguration]. Mirrors the process/resource/management
 * flags `container run` exposes, grouped the same way the CLI reference groups them, so the
 * form reads like a native counterpart to the command line rather than a re-invention of it.
 */
class ContainerRunConfigurationEditor : SettingsEditor<ContainerRunConfiguration>() {

    private val imageField = JBTextField()
    private val nameField = JBTextField()
    private val commandArgsField = JBTextField()
    private val detachCheckbox = JBCheckBox("Detach (-d)")
    private val interactiveCheckbox = JBCheckBox("Interactive (-i)")
    private val ttyCheckbox = JBCheckBox("Allocate TTY (-t)")
    private val removeOnExitCheckbox = JBCheckBox("Remove container after exit (--rm)")
    private val workdirField = JBTextField()
    private val userField = JBTextField()
    private val cpusField = JBTextField()
    private val memoryField = JBTextField()
    private val envArea = JBTextArea(4, 40)
    private val portsArea = JBTextArea(3, 40)
    private val volumesArea = JBTextArea(3, 40)
    private val networksArea = JBTextArea(2, 40)
    private val extraArgsField = JBTextField()

    override fun resetEditorFrom(config: ContainerRunConfiguration) {
        val o = config.options
        imageField.text = o.image
        nameField.text = o.containerName
        commandArgsField.text = o.commandArguments
        detachCheckbox.isSelected = o.detach
        interactiveCheckbox.isSelected = o.interactive
        ttyCheckbox.isSelected = o.tty
        removeOnExitCheckbox.isSelected = o.removeOnExit
        workdirField.text = o.workdir
        userField.text = o.user
        cpusField.text = o.cpus
        memoryField.text = o.memory
        envArea.text = o.envVarsMultiline
        portsArea.text = o.portsMultiline
        volumesArea.text = o.volumesMultiline
        networksArea.text = o.networksMultiline
        extraArgsField.text = o.extraArgs
    }

    override fun applyEditorTo(config: ContainerRunConfiguration) {
        val o = ContainerRunOptions()
        o.image = imageField.text
        o.containerName = nameField.text
        o.commandArguments = commandArgsField.text
        o.detach = detachCheckbox.isSelected
        o.interactive = interactiveCheckbox.isSelected
        o.tty = ttyCheckbox.isSelected
        o.removeOnExit = removeOnExitCheckbox.isSelected
        o.workdir = workdirField.text
        o.user = userField.text
        o.cpus = cpusField.text
        o.memory = memoryField.text
        o.envVarsMultiline = envArea.text
        o.portsMultiline = portsArea.text
        o.volumesMultiline = volumesArea.text
        o.networksMultiline = networksArea.text
        o.extraArgs = extraArgsField.text
        config.setOptions(o)
    }

    override fun createEditor(): JComponent = panel {
        group("Image") {
            row("Image reference:") { cell(imageField).resizableColumn().align(AlignX.FILL) }
            row("Container name:") { cell(nameField).resizableColumn().align(AlignX.FILL) }
            row("Command / arguments:") { cell(commandArgsField).resizableColumn().align(AlignX.FILL) }
        }
        group("Process") {
            row { cell(detachCheckbox) }
            row { cell(interactiveCheckbox) }
            row { cell(ttyCheckbox) }
            row { cell(removeOnExitCheckbox) }
            row("Working directory:") { cell(workdirField).resizableColumn().align(AlignX.FILL) }
            row("User (name|uid[:gid]):") { cell(userField).resizableColumn().align(AlignX.FILL) }
        }
        group("Resources") {
            row("CPUs:") { cell(cpusField).resizableColumn().align(AlignX.FILL) }
            row("Memory (e.g. 512M, 2G):") { cell(memoryField).resizableColumn().align(AlignX.FILL) }
        }
        group("Environment (one KEY=VALUE per line)") {
            row { cell(envArea).resizableColumn().align(AlignX.FILL) }
        }
        group("Ports (one host:container[/proto] per line)") {
            row { cell(portsArea).resizableColumn().align(AlignX.FILL) }
        }
        group("Volumes (one source:target[:ro] per line)") {
            row { cell(volumesArea).resizableColumn().align(AlignX.FILL) }
        }
        group("Networks (one per line)") {
            row { cell(networksArea).resizableColumn().align(AlignX.FILL) }
        }
        group("Advanced") {
            row("Extra CLI arguments:") { cell(extraArgsField).resizableColumn().align(AlignX.FILL) }
        }
    }
}
