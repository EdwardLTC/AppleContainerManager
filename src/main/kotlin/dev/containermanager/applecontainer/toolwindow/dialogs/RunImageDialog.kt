package dev.containermanager.applecontainer.toolwindow.dialogs

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.*
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.table.JBTable
import dev.containermanager.applecontainer.cli.model.ImageInfo
import dev.containermanager.applecontainer.cli.model.NetworkInfo
import dev.containermanager.applecontainer.cli.model.RunSpec
import dev.containermanager.applecontainer.cli.model.VolumeInfo
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*

class RunImageDialog(
    project: Project,
    private val image: ImageInfo,
    volumes: List<VolumeInfo>,
    networks: List<NetworkInfo>,
) : DialogWrapper(project) {

    private val nameField = JBTextField().apply {
        text = image.reference.substringAfterLast("/").substringBeforeLast(":")
        columns = 30
    }

    private val argumentsField = JBTextArea().apply {
        rows = 3
        columns = 40
        lineWrap = true
        wrapStyleWord = true
    }

    private val detachCheckBox = JBCheckBox(
        "Run in background",
        true,
    )

    private val interactiveCheckBox = JBCheckBox(
        "Interactive (-i)",
        false,
    )

    private val ttyCheckBox = JBCheckBox(
        "Allocate TTY (-t)",
        false,
    )

    private val removeOnExitCheckBox = JBCheckBox(
        "Remove container when it exits",
        false,
    )

    private val envField = JBTextArea().apply {
        rows = 4
        columns = 40
        lineWrap = false
    }

    private val envFileField = TextFieldWithBrowseButton().apply {
        val descriptor = FileChooserDescriptor(
            true,
            false,
            false,
            false,
            false,
            false,
        ).apply {
            title = "Select Environment File"
        }

        addBrowseFolderListener(
            TextBrowseFolderListener(descriptor, project)
        )
    }

    private val workdirField = TextFieldWithBrowseButton().apply {
        val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor().apply {
            title = "Select Working Directory"
        }

        addBrowseFolderListener(TextBrowseFolderListener(descriptor, project))
    }

    private val userField = JBTextField().apply {
        columns = 30
    }

    private val limitCpuCheckBox = JBCheckBox(
        "Limit CPUs",
        false,
    )

    private val cpusSpinner = JSpinner(
        SpinnerNumberModel(
            2.0,
            0.5,
            64.0,
            0.5,
        )
    ).apply {
        isEnabled = limitCpuCheckBox.isSelected
    }

    private val limitMemoryCheckBox = JBCheckBox(
        "Limit memory",
        false,
    )

    private val memorySpinner = JSpinner(
        SpinnerNumberModel(
            1024,
            128,
            65536,
            128,
        )
    ).apply {
        isEnabled = limitMemoryCheckBox.isSelected
    }

    private val portTableModel = RunPortTableModel(image.exposedPorts)

    private val portTable = JBTable(portTableModel).apply {
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION)

        setShowGrid(true)
        rowHeight = 28

        columnModel.getColumn(0).preferredWidth = 120
        columnModel.getColumn(1).preferredWidth = 140
        columnModel.getColumn(2).preferredWidth = 100
    }

    private val portsPanel = JPanel(BorderLayout()).apply {

        val scrollPane = JBScrollPane(portTable).apply {
            preferredSize = Dimension(500, 130)
        }

        val toolbar = JPanel().apply {
            layout = BorderLayout()

            val buttons = JPanel()

            val addButton = JButton("Add Port").apply {
                addActionListener {
                    portTableModel.addPort()

                    val lastRow = portTableModel.rowCount - 1

                    if (lastRow >= 0) {
                        portTable.setRowSelectionInterval(
                            lastRow,
                            lastRow,
                        )

                        portTable.editCellAt(
                            lastRow,
                            0,
                        )

                        portTable.editorComponent?.requestFocusInWindow()
                    }
                }
            }

            val removeButton = JButton("Remove").apply {
                addActionListener {
                    val row = portTable.selectedRow

                    if (row >= 0) {
                        portTableModel.removePort(
                            portTable.convertRowIndexToModel(row)
                        )
                    }
                }
            }

            buttons.add(addButton)
            buttons.add(removeButton)

            add(buttons, BorderLayout.WEST)
        }

        add(scrollPane, BorderLayout.CENTER)
        add(toolbar, BorderLayout.SOUTH)
    }

    private val networkTableModel = RunNetworkTableModel(networks)

    private fun createNetworkComboBox(networks: List<NetworkInfo>): ComboBox<NetworkInfo> {
        return ComboBox(networks.toTypedArray()).apply {
            renderer = object : DefaultListCellRenderer() {
                override fun getListCellRendererComponent(
                    list: JList<*>?,
                    value: Any?,
                    index: Int,
                    isSelected: Boolean,
                    cellHasFocus: Boolean,
                ): Component {
                    super.getListCellRendererComponent(
                        list,
                        value,
                        index,
                        isSelected,
                        cellHasFocus,
                    )

                    text = (value as? NetworkInfo)?.name.orEmpty()

                    return this
                }
            }
        }
    }

    private val networkComboBox = createNetworkComboBox(networks)

    private val networkTable = JBTable(networkTableModel).apply {
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        rowHeight = 28
        setShowGrid(true)
        columnModel.getColumn(0).preferredWidth = 300
        columnModel.getColumn(0).cellEditor = DefaultCellEditor(networkComboBox)
    }

    private val networksPanel = JPanel(BorderLayout()).apply {
        val scrollPane = JBScrollPane(
            networkTable
        ).apply {
            preferredSize = Dimension(
                500,
                100,
            )
        }
        val buttons = JPanel(
            FlowLayout(
                FlowLayout.LEFT,
                4,
                4,
            )
        )

        val addButton = JButton("Add").apply {
            addActionListener {

                networkTableModel.addNetwork()

                val row =
                    networkTableModel.rowCount - 1

                if (row >= 0) {
                    networkTable
                        .setRowSelectionInterval(
                            row,
                            row,
                        )
                }
            }
        }

        val removeButton = JButton("Remove").apply {
            addActionListener {

                val selectedRow =
                    networkTable.selectedRow

                if (selectedRow >= 0) {

                    val modelRow =
                        networkTable
                            .convertRowIndexToModel(
                                selectedRow
                            )

                    networkTableModel
                        .removeNetwork(modelRow)
                }
            }
        }

        buttons.add(addButton)
        buttons.add(removeButton)

        add(
            scrollPane,
            BorderLayout.CENTER,
        )

        add(
            buttons,
            BorderLayout.SOUTH,
        )
    }

    private val dnsField = JBTextArea().apply {
        rows = 2
        columns = 40
        lineWrap = false
    }

    private val mountTableModel = RunMountTableModel(volumes)

    private val mountTypeComboBox = ComboBox(MountType.entries.toTypedArray()).apply {
        renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean,
            ): Component {
                super.getListCellRendererComponent(
                    list,
                    value,
                    index,
                    isSelected,
                    cellHasFocus,
                )

                text = when (value) {
                    MountType.VOLUME -> "Volume"
                    MountType.BIND -> "Bind mount"
                    else -> ""
                }

                return this
            }
        }
    }

    private val mountTable = JBTable(mountTableModel).apply {
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION)

        setShowGrid(true)
        rowHeight = 28

        columnModel.getColumn(0).preferredWidth = 100
        columnModel.getColumn(1).preferredWidth = 220
        columnModel.getColumn(2).preferredWidth = 180
        columnModel.getColumn(3).preferredWidth = 80

        columnModel.getColumn(0).cellEditor = DefaultCellEditor(
            mountTypeComboBox
        )

        columnModel.getColumn(3).cellEditor = DefaultCellEditor(JCheckBox())
    }

    private val mountsPanel = JPanel(BorderLayout()).apply {

        val scrollPane = JBScrollPane(mountTable).apply {
            preferredSize = Dimension(600, 160)
        }

        val buttons = JPanel()

        val addButton = JButton("Add").apply {
            addActionListener {
                mountTableModel.addMount()

                val row = mountTableModel.rowCount - 1

                if (row >= 0) {
                    mountTable.setRowSelectionInterval(row, row)
                }
            }
        }

        val removeButton = JButton("Remove").apply {
            addActionListener {
                val selectedRow = mountTable.selectedRow

                if (selectedRow >= 0) {
                    val modelRow =
                        mountTable.convertRowIndexToModel(selectedRow)

                    mountTableModel.removeMount(modelRow)
                }
            }
        }

        buttons.add(addButton)
        buttons.add(removeButton)

        add(scrollPane, BorderLayout.CENTER)
        add(buttons, BorderLayout.SOUTH)
    }

    private val labelsField = JBTextArea().apply {
        rows = 3
        columns = 40
        lineWrap = false
    }

    private val capAddField = JBTextArea().apply {
        rows = 2
        columns = 40
        lineWrap = false
    }

    private val capDropField = JBTextArea().apply {
        rows = 2
        columns = 40
        lineWrap = false
    }

    private val entrypointField = JBTextField().apply {
        columns = 30
    }

    private val archField = JBTextField().apply {
        columns = 20
    }

    private val osField = JBTextField().apply {
        columns = 20
    }

    private val platformField = JBTextField().apply {
        columns = 20
    }

    private val initCheckBox = JBCheckBox(
        "Initialize with init process",
        false,
    )

    private val rosettaCheckBox = JBCheckBox(
        "Enable rosetta",
        false,
    )

    private val readOnlyCheckBox = JBCheckBox(
        "Read-only filesystem",
        false,
    )

    init {
        title = "Run ${image.reference}"

        init()

        isResizable = true
    }

    init {
        limitCpuCheckBox.addActionListener {
            cpusSpinner.isEnabled = limitCpuCheckBox.isSelected
        }

        limitMemoryCheckBox.addActionListener {
            memorySpinner.isEnabled = limitMemoryCheckBox.isSelected
        }

        title = "Run ${image.reference}"

        init()

        isResizable = true
    }

    override fun createCenterPanel(): JComponent {
        val content = panel {

            group("General") {

                row("Image:") {
                    label(image.reference).resizableColumn()
                }

                row("Name:") {
                    cell(nameField).align(AlignX.FILL)
                }

                row("Arguments:") {
                    scrollCell(argumentsField).align(AlignX.FILL)
                }

                row {
                    cell(detachCheckBox)
                }

                row {
                    cell(interactiveCheckBox)
                    cell(ttyCheckBox)
                }

                row {
                    cell(removeOnExitCheckBox)
                }
            }

            group("Environment") {

                row("Environment:") {
                    scrollCell(envField).align(AlignX.FILL)
                }

                row("Env file:") {
                    cell(envFileField).align(AlignX.FILL)
                }

                row("Working directory:") {
                    cell(workdirField).align(AlignX.FILL)
                }

                row("User:") {
                    cell(userField).align(AlignX.FILL)
                }
            }

            group("Resources") {

                row("CPUs:") {
                    cell(limitCpuCheckBox)
                    cell(cpusSpinner)
                    label("Cores")
                }

                row("Memory:") {
                    cell(limitMemoryCheckBox)
                    cell(memorySpinner)
                    label("MiB")
                }
            }

            group("Networking") {

                row {
                    cell(
                        JPanel(BorderLayout()).apply {
                            add(JBLabel("Ports:"), BorderLayout.NORTH)
                            add(portsPanel, BorderLayout.CENTER)
                        }
                    ).align(AlignX.FILL)
                }

                row {
                    cell(JPanel(BorderLayout()).apply {
                        add(JBLabel("Networks:"), BorderLayout.NORTH)
                        add(networksPanel, BorderLayout.CENTER)
                    }).align(AlignX.FILL)
                }
            }

            group("Storage") {
                row {
                    cell(mountsPanel)
                        .align(AlignX.FILL)
                }
            }

            group("Advanced") {

                row("Labels:") {
                    scrollCell(labelsField).align(AlignX.FILL)
                }

                row("Capabilities +:") {
                    scrollCell(capAddField).align(AlignX.FILL)
                }

                row("Capabilities -:") {
                    scrollCell(capDropField).align(AlignX.FILL)
                }

                row("Entrypoint:") {
                    cell(entrypointField).align(AlignX.FILL)
                }

                row("Architecture:") {
                    cell(archField).align(AlignX.FILL)
                }

                row("OS:") {
                    cell(osField).align(AlignX.FILL)
                }

                row("Platform:") {
                    cell(platformField).align(AlignX.FILL)
                }

                row {
                    cell(initCheckBox)
                }

                row {
                    cell(rosettaCheckBox)
                }

                row {
                    cell(readOnlyCheckBox)
                }
            }
        }

        return JBScrollPane(content).apply {
            preferredSize = Dimension(650, 550)
        }
    }

    fun runSpec(): RunSpec {
        return RunSpec(
            image = image.reference,
            arguments = argumentsField.text.toLines(),
            name = nameField.text.trim().ifBlank { null },
            detach = detachCheckBox.isSelected,
            interactive = interactiveCheckBox.isSelected,
            tty = ttyCheckBox.isSelected,
            removeOnExit = removeOnExitCheckBox.isSelected,
            env = envField.text.toLines(),
            envFile = envFileField.text.trim().ifBlank { null },
            workdir = workdirField.text.trim().ifBlank { null },
            user = userField.text.trim().ifBlank { null },
            cpus = if (limitCpuCheckBox.isSelected) {
                cpusSpinner.value
                    .toString()
                    .toDouble()
                    .let {
                        if (it % 1.0 == 0.0) {
                            it.toInt().toString()
                        } else {
                            it.toString()
                        }
                    }
            } else {
                null
            },
            memory = if (limitMemoryCheckBox.isSelected) {
                "${memorySpinner.value}MiB"
            } else {
                null
            },
            ports = portTableModel.toRunSpecPorts(),
            volumes = mountTableModel.toVolumes(),
            mounts = mountTableModel.toMounts(),
            networks = networkTableModel.networks(),
            dnsServers = dnsField.text.toLines(),
            labels = labelsField.text.toLines(),
            capAdd = capAddField.text.toLines(),
            capDrop = capDropField.text.toLines(),
            entrypoint = entrypointField.text.trim().ifBlank { null },
            arch = archField.text.trim().ifBlank { null },
            os = osField.text.trim().ifBlank { null },
            platform = platformField.text.trim().ifBlank { null },
            init = initCheckBox.isSelected,
            rosetta = rosettaCheckBox.isSelected,
            readOnly = readOnlyCheckBox.isSelected,
        )
    }

    private fun String.toLines(): List<String> = lines().map(String::trim).filter(String::isNotEmpty)
}