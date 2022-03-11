package com.launchdarkly.intellij.toolwindow

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory

class FlagToolWindow(project: Project) : DumbAware, Disposable {
    private val basePanel: BasePanel = BasePanel(project)
    fun initializePanel(toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.SERVICE.getInstance()

        val content: Content = contentFactory.createContent(null, null, false)
        content.component = basePanel

        toolWindow.contentManager.addContent(content)
    }

    fun getPanel(): BasePanel {
        return basePanel
    }

    companion object {
        fun getInstance(project: Project): FlagToolWindow =
            project.getService(FlagToolWindow::class.java)
    }

    override fun dispose() {}
}
