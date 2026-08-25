package dev.containermanager.applecontainer.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Application-scoped coroutine launcher for action handlers that need to run a suspend CLI
 * call off the EDT and report success/failure back via a callback. The IntelliJ platform
 * injects and manages the [CoroutineScope]'s lifecycle (cancelled on plugin unload).
 */
@Service(Service.Level.PROJECT)
class PluginScopeService(private val scope: CoroutineScope) {

    fun launchIo(onError: ((Throwable) -> Unit)? = null, block: suspend () -> Unit): Job =
        scope.launch(Dispatchers.IO) {
            try {
                block()
            } catch (t: Throwable) {
                onError?.invoke(t)
            }
        }

    companion object {
        fun getInstance(project: Project): PluginScopeService =
            project.getService(PluginScopeService::class.java)
    }
}
