package com.launchdarkly.intellij.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.launchdarkly.intellij.toolwindow.FlagToolWindow

object Utils {
    fun getSelectedNode(event: AnActionEvent): Any? {
        val ideProject = event.project ?: return null
        return ideProject.service<FlagToolWindow>().getPanel().getFlagPanel().tree.lastSelectedPathComponent
    }

    fun updateNode(e: AnActionEvent, nodePrefix: String) {
        val selectedNode = getSelectedNode(e) ?: return
        e.presentation.isEnabledAndVisible =
            e.presentation.isEnabled && (selectedNode.toString().startsWith(nodePrefix))
    }
}