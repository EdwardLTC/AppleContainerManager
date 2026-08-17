package dev.containermanager.applecontainer.run

import com.intellij.util.xmlb.annotations.Tag
import dev.containermanager.applecontainer.cli.model.RunSpec

/**
 * Flat, XML-serializable mirror of [RunSpec]. Kept separate from the domain [RunSpec] so the
 * persisted run-configuration schema doesn't have to track every field the CLI layer models
 * (lists-of-strings serialize awkwardly with IntelliJ's PersistentStateComponent XML binding,
 * so multi-value fields are stored as newline-delimited text and split on read).
 */
@Tag("ContainerRunOptions")
class ContainerRunOptions {
    var image: String = ""
    var containerName: String = ""
    var commandArguments: String = ""
    var detach: Boolean = false
    var interactive: Boolean = true
    var tty: Boolean = true
    var removeOnExit: Boolean = true
    var workdir: String = ""
    var user: String = ""
    var cpus: String = ""
    var memory: String = ""
    var envVarsMultiline: String = ""
    var portsMultiline: String = ""
    var volumesMultiline: String = ""
    var networksMultiline: String = ""
    var extraArgs: String = ""

    private fun lines(text: String): List<String> =
        text.lines().map { it.trim() }.filter { it.isNotEmpty() }

    fun toRunSpec(): RunSpec = RunSpec(
        image = image.trim(),
        arguments = commandArguments.trim().takeIf { it.isNotEmpty() }?.split(Regex("\\s+")) ?: emptyList(),
        name = containerName.trim().takeIf { it.isNotEmpty() },
        detach = detach,
        interactive = interactive,
        tty = tty,
        removeOnExit = removeOnExit,
        workdir = workdir.trim().takeIf { it.isNotEmpty() },
        user = user.trim().takeIf { it.isNotEmpty() },
        cpus = cpus.trim().takeIf { it.isNotEmpty() },
        memory = memory.trim().takeIf { it.isNotEmpty() },
        env = lines(envVarsMultiline),
        ports = lines(portsMultiline),
        volumes = lines(volumesMultiline),
        networks = lines(networksMultiline),
        extraArgs = extraArgs.trim().takeIf { it.isNotEmpty() }?.split(Regex("\\s+")) ?: emptyList(),
    )

    fun copy(): ContainerRunOptions {
        val other = ContainerRunOptions()
        other.image = image
        other.containerName = containerName
        other.commandArguments = commandArguments
        other.detach = detach
        other.interactive = interactive
        other.tty = tty
        other.removeOnExit = removeOnExit
        other.workdir = workdir
        other.user = user
        other.cpus = cpus
        other.memory = memory
        other.envVarsMultiline = envVarsMultiline
        other.portsMultiline = portsMultiline
        other.volumesMultiline = volumesMultiline
        other.networksMultiline = networksMultiline
        other.extraArgs = extraArgs
        return other
    }
}
