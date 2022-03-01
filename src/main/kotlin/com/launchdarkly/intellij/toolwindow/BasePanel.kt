package com.launchdarkly.intellij.toolwindow

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.OnePixelSplitter
import com.launchdarkly.intellij.messaging.DefaultMessageBusService
import java.awt.BorderLayout
import javax.swing.JPanel

class BasePanel(project: Project) : JPanel() {
    private val messageBus = project.service<DefaultMessageBusService>()
    private val splitter = OnePixelSplitter(true, "LDSplitterProportion", .25f)
    private val flagPanel = FlagPanel(project, messageBus)
    private val linkPanel = LinkPanel(project, messageBus)

    init {
        layout = BorderLayout(0, 0)
        splitter.apply {
            setResizeEnabled(false)
            firstComponent = linkPanel
            secondComponent = flagPanel
        }
        add(splitter, BorderLayout.CENTER)
    }

    fun getFlagPanel(): FlagPanel {
        return flagPanel
    }
}