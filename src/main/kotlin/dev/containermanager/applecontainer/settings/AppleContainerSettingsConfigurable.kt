package dev.containermanager.applecontainer.settings

import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

class AppleContainerSettingsConfigurable : Configurable {

    private var component: AppleContainerSettingsComponent? = null

    override fun getDisplayName(): String = "Apple Container Manager"

    override fun createComponent(): JComponent {
        val c = AppleContainerSettingsComponent()
        component = c
        return c.panel
    }

    override fun isModified(): Boolean {
        val c = component ?: return false
        val state = AppleContainerSettingsState.getInstance()
        return c.cliPath != state.cliPath ||
            c.pollIntervalSeconds != state.pollIntervalSeconds ||
            c.autoRefreshEnabled != state.autoRefreshEnabled ||
            c.confirmDestructiveActions != state.confirmDestructiveActions
    }

    override fun apply() {
        val c = component ?: return
        val state = AppleContainerSettingsState.getInstance()
        state.cliPath = c.cliPath
        state.pollIntervalSeconds = c.pollIntervalSeconds
        state.autoRefreshEnabled = c.autoRefreshEnabled
        state.confirmDestructiveActions = c.confirmDestructiveActions
    }

    override fun reset() {
        val c = component ?: return
        val state = AppleContainerSettingsState.getInstance()
        c.cliPath = state.cliPath
        c.pollIntervalSeconds = state.pollIntervalSeconds
        c.autoRefreshEnabled = state.autoRefreshEnabled
        c.confirmDestructiveActions = state.confirmDestructiveActions
    }

    override fun getPreferredFocusedComponent(): JComponent? = component?.preferredFocusedComponent()

    override fun disposeUIResources() {
        component = null
    }
}
