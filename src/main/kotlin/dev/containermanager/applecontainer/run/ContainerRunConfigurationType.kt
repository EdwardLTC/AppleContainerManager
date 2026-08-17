package dev.containermanager.applecontainer.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project

class ContainerRunConfigurationType : ConfigurationTypeBase(
    "AppleContainerRunConfiguration",
    "Apple Container",
    "Run a container image with Apple's container runtime",
    AllIcons.RunConfigurations.Application,
) {
    init {
        addFactory(ContainerRunConfigurationFactory(this))
    }
}

class ContainerRunConfigurationFactory(type: ContainerRunConfigurationType) : ConfigurationFactory(type) {

    override fun getId(): String = "AppleContainerRunConfigurationFactory"

    override fun createTemplateConfiguration(project: Project): RunConfiguration =
        ContainerRunConfiguration(project, this, "Container")
}
