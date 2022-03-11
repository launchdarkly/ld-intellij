package com.launchdarkly.intellij.toolwindow

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.OnePixelSplitter
import com.launchdarkly.intellij.messaging.DefaultMessageBusService
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.border.Border

class BasePanel(project: Project) : JPanel() {
    private val messageBus = project.service<DefaultMessageBusService>()
    private val splitter = OnePixelSplitter(true, "LDSplitterProportion", .25f)
    private val flagPanel = FlagPanel(project, messageBus)
    private val linkPanel = LinkPanel(project, messageBus)

    init {
        val quickLinksTitle: Border = BorderFactory.createTitledBorder("Quick Links")
        val flagsTitle: Border = BorderFactory.createTitledBorder("Feature Flags")

        linkPanel.border = quickLinksTitle
        flagPanel.border = flagsTitle
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
