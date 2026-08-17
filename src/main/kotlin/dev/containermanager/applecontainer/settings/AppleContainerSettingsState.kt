package dev.containermanager.applecontainer.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

data class AppleContainerSettingsData(
    var cliPath: String = "",
    var pollIntervalSeconds: Int = 5,
    var autoRefreshEnabled: Boolean = true,
    var defaultRegistryScheme: String = "auto",
    var confirmDestructiveActions: Boolean = true,
)

@Service(Service.Level.APP)
@State(name = "AppleContainerManagerSettings", storages = [Storage("apple-container-manager.xml")])
class AppleContainerSettingsState : PersistentStateComponent<AppleContainerSettingsData> {

    private var data = AppleContainerSettingsData()

    override fun getState(): AppleContainerSettingsData = data

    override fun loadState(state: AppleContainerSettingsData) {
        XmlSerializerUtil.copyBean(state, data)
    }

    var cliPath: String
        get() = data.cliPath
        set(value) { data.cliPath = value }

    var pollIntervalSeconds: Int
        get() = data.pollIntervalSeconds
        set(value) { data.pollIntervalSeconds = value }

    var autoRefreshEnabled: Boolean
        get() = data.autoRefreshEnabled
        set(value) { data.autoRefreshEnabled = value }

    var defaultRegistryScheme: String
        get() = data.defaultRegistryScheme
        set(value) { data.defaultRegistryScheme = value }

    var confirmDestructiveActions: Boolean
        get() = data.confirmDestructiveActions
        set(value) { data.confirmDestructiveActions = value }

    companion object {
        fun getInstance(): AppleContainerSettingsState =
            ApplicationManager.getApplication().getService(AppleContainerSettingsState::class.java)
    }
}
