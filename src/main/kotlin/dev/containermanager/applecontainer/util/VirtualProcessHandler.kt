package dev.containermanager.applecontainer.util

import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputTypes
import java.io.OutputStream

/**
 * A [ProcessHandler] with no real OS process behind it. Used where an action drives several
 * `container` CLI invocations itself (see
 * [dev.containermanager.applecontainer.compose.ComposeOrchestrator]) but still wants to present
 * output through IntelliJ's normal Run console, complete with a working Stop button.
 */
class VirtualProcessHandler : ProcessHandler() {

    /** Invoked when the user hits Stop in the console; wire this to cancel the backing coroutine. */
    var onStopRequested: (() -> Unit)? = null

    override fun destroyProcessImpl() {
        onStopRequested?.invoke()
        notifyProcessTerminated(1)
    }

    override fun detachProcessImpl() {
        notifyProcessDetached()
    }

    override fun detachIsDefault(): Boolean = false

    override fun getProcessInput(): OutputStream? = null

    fun println(text: String) = notifyTextAvailable("$text\n", ProcessOutputTypes.STDOUT)

    fun printlnError(text: String) = notifyTextAvailable("$text\n", ProcessOutputTypes.STDERR)

    fun finish(exitCode: Int) {
        if (!isProcessTerminated) notifyProcessTerminated(exitCode)
    }
}