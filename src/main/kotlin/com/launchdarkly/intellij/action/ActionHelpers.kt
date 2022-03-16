package com.launchdarkly.intellij.action

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.launchdarkly.intellij.toolwindow.FlagToolWindow
import javax.swing.tree.DefaultMutableTreeNode

object ActionHelpers {
    fun getLastSelectedDefaultMutableTreeNode(project: Project): DefaultMutableTreeNode? {
        return project.service<FlagToolWindow>().getPanel().getFlagPanel().tree.lastSelectedPathComponent as? DefaultMutableTreeNode
    }
}
