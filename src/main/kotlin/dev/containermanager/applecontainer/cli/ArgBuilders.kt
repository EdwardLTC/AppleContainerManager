package dev.containermanager.applecontainer.cli

import dev.containermanager.applecontainer.cli.model.BuildSpec
import dev.containermanager.applecontainer.cli.model.RunSpec

/**
 * Translates typed specs ([RunSpec], [BuildSpec]) into `container` CLI argument lists.
 * Keeping this isolated means the Run Configuration editor, quick-run actions, and any
 * future "run this Dockerfile" gutter action all produce identical, correct command lines.
 */
object ArgBuilders {

    fun runArgs(spec: RunSpec, subcommand: String = "run"): List<String> = buildList {
        add(subcommand)
        if (subcommand == "run") {
            if (spec.detach) add("--detach")
        }
        if (spec.interactive) add("--interactive")
        if (spec.tty) add("--tty")
        if (spec.removeOnExit) add("--rm")
        spec.name?.let { addAll(listOf("--name", it)) }
        spec.workdir?.let { addAll(listOf("--workdir", it)) }
        spec.user?.let { addAll(listOf("--user", it)) }
        spec.cpus?.let { addAll(listOf("--cpus", it)) }
        spec.memory?.let { addAll(listOf("--memory", it)) }
        spec.entrypoint?.let { addAll(listOf("--entrypoint", it)) }
        spec.arch?.let { addAll(listOf("--arch", it)) }
        spec.os?.let { addAll(listOf("--os", it)) }
        spec.platform?.let { addAll(listOf("--platform", it)) }
        if (spec.init) add("--init")
        if (spec.rosetta) add("--rosetta")
        if (spec.readOnly) add("--read-only")
        spec.envFile?.let { addAll(listOf("--env-file", it)) }
        spec.env.forEach { addAll(listOf("--env", it)) }
        spec.ports.forEach { addAll(listOf("--publish", it)) }
        spec.volumes.forEach { addAll(listOf("--volume", it)) }
        spec.mounts.forEach { addAll(listOf("--mount", it)) }
        spec.networks.forEach { addAll(listOf("--network", it)) }
        spec.dnsServers.forEach { addAll(listOf("--dns", it)) }
        spec.labels.forEach { addAll(listOf("--label", it)) }
        spec.capAdd.forEach { addAll(listOf("--cap-add", it)) }
        spec.capDrop.forEach { addAll(listOf("--cap-drop", it)) }
        addAll(spec.extraArgs)
        add(spec.image)
        addAll(spec.arguments)
    }

    fun buildArgs(spec: BuildSpec): List<String> = buildList {
        add("build")
        spec.dockerfile?.let { addAll(listOf("--file", it)) }
        spec.tags.forEach { addAll(listOf("--tag", it)) }
        spec.buildArgs.forEach { addAll(listOf("--build-arg", it)) }
        spec.target?.let { addAll(listOf("--target", it)) }
        if (spec.noCache) add("--no-cache")
        if (spec.pull) add("--pull")
        spec.platform?.let { addAll(listOf("--platform", it)) }
        spec.cpus?.let { addAll(listOf("--cpus", it)) }
        spec.memory?.let { addAll(listOf("--memory", it)) }
        spec.labels.forEach { addAll(listOf("--label", it)) }
        add("--progress")
        add("plain")
        addAll(spec.extraArgs)
        add(spec.contextDir)
    }
}
