package com.launchdarkly.intellij.toolwindow

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.launchdarkly.intellij.messaging.DefaultMessageBusService

class FlagToolWindow(project: Project) : DumbAware, Disposable {
    private val messageBus = project.service<DefaultMessageBusService>()
    private val flagPanel: FlagPanel = FlagPanel(project, messageBus)

    fun initializePanel(toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.SERVICE.getInstance()

        val content: Content = contentFactory.createContent(null, null, false)
        content.component = flagPanel

        Disposer.register(this, flagPanel)
        toolWindow.contentManager.addContent(content)
    }

    fun getPanel(): FlagPanel {
        return flagPanel
    }

    companion object {
        fun getInstance(project: Project): FlagToolWindow =
            project.getService(FlagToolWindow::class.java)
    }

    override fun dispose() {}
}
