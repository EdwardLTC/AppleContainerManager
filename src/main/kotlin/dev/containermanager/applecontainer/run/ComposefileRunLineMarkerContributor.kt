package dev.containermanager.applecontainer.run

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.psi.PsiElement
import dev.containermanager.applecontainer.actions.compose.COMPOSE_FILE_NAMES
import dev.containermanager.applecontainer.actions.compose.composeFileFrom
import dev.containermanager.applecontainer.actions.compose.runCompose

class ComposeFileRunLineMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        if (element.textOffset != 0) return null
        val file = element.containingFile?.virtualFile ?: return null
        if (!isContainerBuildFile(file.name)) return null

        return Info(
            AllIcons.Actions.Compile,
            arrayOf(BuildFromComposeFileAction()),
        ) { "Build with Apple Container" }
    }

    private fun isContainerBuildFile(name: String): Boolean = name in COMPOSE_FILE_NAMES
}

private class BuildFromComposeFileAction :
    AnAction("Build with Apple Container", "Build an image from this Compose file", AllIcons.Actions.Compile) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = composeFileFrom(e) ?: return

        runCompose(project, file, title = "Apple Compose: up") { orchestrator, compose, onLine ->
            orchestrator.up(compose, onLine)
        }

    }
}
