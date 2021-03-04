package com.launchdarkly.intellij.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory


class FlagToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val flagExplorer = FlagToolWindow.getInstance(project)
        flagExplorer.initializePanel(toolWindow)

    }

}