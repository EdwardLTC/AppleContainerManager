package dev.containermanager.applecontainer.toolwindow.components

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightVirtualFile

/** Opens read-only JSON in the IDE editor with its normal plain-text tooling. */
fun openJsonPreview(project: Project, name: String, content: String) {
    ApplicationManager.getApplication().invokeLater {
        val file = LightVirtualFile(name, PlainTextFileType.INSTANCE, content)
        file.isWritable = false
        FileEditorManager.getInstance(project).openFile(file, true)
    }
}
