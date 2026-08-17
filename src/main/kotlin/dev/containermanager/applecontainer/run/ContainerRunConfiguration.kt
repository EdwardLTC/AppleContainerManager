package dev.containermanager.applecontainer.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.InvalidDataException
import com.intellij.openapi.util.WriteExternalException
import com.intellij.util.xmlb.XmlSerializer
import org.jdom.Element

class ContainerRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    RunConfigurationBase<Element>(project, factory, name) {

    var options: ContainerRunOptions = ContainerRunOptions()
        private set

    override fun getConfigurationEditor(): SettingsEditor<out ContainerRunConfiguration> =
        ContainerRunConfigurationEditor()

    override fun checkConfiguration() {
        if (options.image.isBlank()) {
            throw RuntimeConfigurationError("Specify an image reference to run (e.g. alpine:latest)")
        }
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
        ContainerCommandLineState(environment, options.copy())

    @Throws(InvalidDataException::class)
    override fun readExternal(element: Element) {
        super.readExternal(element)
        val optionsElement = element.getChild("ContainerRunOptions")
        if (optionsElement != null) {
            options = ContainerRunOptions()
            XmlSerializer.deserializeInto(options, optionsElement)
        }
    }

    @Throws(WriteExternalException::class)
    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        val optionsElement = Element("ContainerRunOptions")
        XmlSerializer.serializeInto(options, optionsElement)
        element.addContent(optionsElement)
    }

    fun setOptions(newOptions: ContainerRunOptions) {
        options = newOptions
    }
}
