package dev.containermanager.applecontainer.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import dev.containermanager.applecontainer.cli.CliLocator
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

class AppleContainerSettingsComponent {

    private val cliPathField = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener(
            "Select `container` Executable",
            "Path to Apple's container CLI binary",
            null,
            FileChooserDescriptorFactory.createSingleFileDescriptor(),
        )
    }

    private val detectedPathLabel = JBLabel().apply {
        foreground = com.intellij.ui.JBColor.GRAY
    }

    private val pollIntervalSpinner = JSpinner(SpinnerNumberModel(5, 1, 300, 1))
    private val autoRefreshCheckbox = JBCheckBox("Automatically refresh tool window")
    private val confirmDestructiveCheckbox = JBCheckBox("Confirm before stop / kill / delete")

    val panel: JPanel = FormBuilder.createFormBuilder()
        .addLabeledComponent("Container CLI path:", cliPathField)
        .addComponentToRightColumn(detectedPathLabel)
        .addLabeledComponent("Auto-refresh interval (seconds):", pollIntervalSpinner)
        .addComponent(autoRefreshCheckbox)
        .addComponent(confirmDestructiveCheckbox)
        .addComponentFillVertically(JPanel(), 0)
        .panel

    init {
        refreshDetectedPathLabel()
        cliPathField.textField.document.addDocumentListenerCompat { refreshDetectedPathLabel() }
    }

    private fun refreshDetectedPathLabel() {
        val resolved = CliLocator.resolve(cliPathField.text.ifBlank { null })
        detectedPathLabel.text = if (resolved != null) {
            "Resolved: $resolved"
        } else {
            "Could not locate the `container` binary. Install it from github.com/apple/container or set the path above."
        }
    }

    var cliPath: String
        get() = cliPathField.text
        set(value) { cliPathField.text = value }

    var pollIntervalSeconds: Int
        get() = pollIntervalSpinner.value as Int
        set(value) { pollIntervalSpinner.value = value }

    var autoRefreshEnabled: Boolean
        get() = autoRefreshCheckbox.isSelected
        set(value) { autoRefreshCheckbox.isSelected = value }

    var confirmDestructiveActions: Boolean
        get() = confirmDestructiveCheckbox.isSelected
        set(value) { confirmDestructiveCheckbox.isSelected = value }

    fun preferredFocusedComponent(): JComponent = cliPathField
}

private fun javax.swing.text.Document.addDocumentListenerCompat(onChange: () -> Unit) {
    addDocumentListener(object : javax.swing.event.DocumentListener {
        override fun insertUpdate(e: javax.swing.event.DocumentEvent) = onChange()
        override fun removeUpdate(e: javax.swing.event.DocumentEvent) = onChange()
        override fun changedUpdate(e: javax.swing.event.DocumentEvent) = onChange()
    })
}
