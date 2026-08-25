package dev.containermanager.applecontainer.services

import com.intellij.execution.services.ServiceEventListener
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import dev.containermanager.applecontainer.cli.AppleContainerCli
import dev.containermanager.applecontainer.cli.CliLocator
import dev.containermanager.applecontainer.cli.model.ContainerInfo
import dev.containermanager.applecontainer.cli.model.ImageInfo
import dev.containermanager.applecontainer.cli.model.NetworkInfo
import dev.containermanager.applecontainer.cli.model.VolumeInfo
import dev.containermanager.applecontainer.services.view.AppleContainerServiceViewContributor
import dev.containermanager.applecontainer.settings.AppleContainerSettingsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.milliseconds

/** Snapshot of everything the Services view renders, refreshed together to keep nodes consistent. */
data class RuntimeSnapshot(
    val containers: List<ContainerInfo> = emptyList(),
    val images: List<ImageInfo> = emptyList(),
    val volumes: List<VolumeInfo> = emptyList(),
    val networks: List<NetworkInfo> = emptyList(),
    val cliAvailable: Boolean = true,
    val daemonRunning: Boolean = true,
    val lastError: String? = null,
    val isRefreshing: Boolean = false,
)

/**
 * Central project service: owns the single [AppleContainerCli] instance for this project,
 * polls it on a background coroutine, and publishes a [RuntimeSnapshot] the Services view and
 * any other UI subscribes to via [StateFlow]. Nothing here ever touches the EDT directly —
 * consumers (Swing panels) collect the flow and marshal updates to the EDT themselves.
 */
@Service(Service.Level.PROJECT)
class ContainerRuntimeService(private val project: Project, private val scope: CoroutineScope) {

    private val logger = thisLogger()
    private val refreshMutex = Mutex()

    private val _snapshot = MutableStateFlow(RuntimeSnapshot())
    val snapshot: StateFlow<RuntimeSnapshot> = _snapshot.asStateFlow()

    val cli: AppleContainerCli = AppleContainerCli(
        binaryPathProvider = { CliLocator.resolve(AppleContainerSettingsState.getInstance().cliPath) },
    )

    private var pollingJob: kotlinx.coroutines.Job? = null

    init {
        startPolling()
    }

    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = scope.launch(Dispatchers.IO) {
            while (true) {
                val settings = AppleContainerSettingsState.getInstance()
                if (settings.autoRefreshEnabled) {
                    refresh()
                }
                delay(((settings.pollIntervalSeconds.coerceAtLeast(1)) * 1000L).milliseconds)
            }
        }
    }

    fun requestRefresh() {
        scope.launch(Dispatchers.IO) { refresh() }
    }

    fun resetSnapshot() {
        _snapshot.value = RuntimeSnapshot()
    }

    fun isServicesRunning(): Boolean = _snapshot.value.daemonRunning

    private suspend fun refresh() {
        // Avoid overlapping refreshes if a manual refresh races the poller.
        if (refreshMutex.isLocked) return
        refreshMutex.withLock {
            _snapshot.value = _snapshot.value.copy(isRefreshing = true)
            try {
                val available = cli.isCliAvailable()
                if (!available) {
                    _snapshot.value = _snapshot.value.copy(
                        cliAvailable = false,
                        isRefreshing = false,
                        lastError = "The `container` CLI was not found.",
                    )
                    return
                }
                val daemonRunning = cli.isDaemonRunning()
                if (!daemonRunning) {
                    _snapshot.value = _snapshot.value.copy(
                        cliAvailable = true,
                        daemonRunning = false,
                        isRefreshing = false,
                        lastError = null,
                    )
                    return
                }

                val containers = runCatching { cli.containers.list(all = true) }.getOrElse {
                    logger.warn("Failed to list containers", it); emptyList()
                }
                val images = runCatching { cli.images.list() }.getOrElse {
                    logger.warn("Failed to list images", it); emptyList()
                }
                val volumes = runCatching { cli.volumes.list() }.getOrElse {
                    logger.warn("Failed to list volumes", it); emptyList()
                }
                val networks = runCatching { cli.networks.list() }.getOrElse {
                    logger.warn("Failed to list networks", it); emptyList()
                }

                _snapshot.value = RuntimeSnapshot(
                    containers = containers,
                    images = images,
                    volumes = volumes,
                    networks = networks,
                    cliAvailable = true,
                    daemonRunning = true,
                    lastError = null,
                    isRefreshing = false,
                )
                notifyServicesViewChanged()
            } catch (t: Throwable) {
                logger.warn("Runtime refresh failed", t)
                _snapshot.value = _snapshot.value.copy(isRefreshing = false, lastError = t.message)
            }
        }
    }

    /** Tells the "Apple Container" node in the Services view to re-query and redraw its tree. */
    private fun notifyServicesViewChanged() {
        project.messageBus.syncPublisher(ServiceEventListener.TOPIC)
            .handle(ServiceEventListener.ServiceEvent.createResetEvent(AppleContainerServiceViewContributor::class.java))
    }

    companion object {
        fun getInstance(project: Project): ContainerRuntimeService =
            project.getService(ContainerRuntimeService::class.java)
    }
}