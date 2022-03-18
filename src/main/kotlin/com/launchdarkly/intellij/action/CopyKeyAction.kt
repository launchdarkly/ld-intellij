package com.launchdarkly.intellij.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.launchdarkly.intellij.notifications.Notifier
import com.launchdarkly.intellij.toolwindow.FlagNodeParent
import com.launchdarkly.intellij.toolwindow.KEY_PREFIX
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection
import javax.swing.tree.DefaultMutableTreeNode

class CopyKeyAction : AnAction() {
    companion object {
        const val ID = "com.launchdarkly.intellij.action.CopyKeyAction"
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        var selectedNode = ActionHelpers.getLastSelectedDefaultMutableTreeNode(project)
        while (selectedNode != null) {
            if (selectedNode.userObject is FlagNodeParent) {
                val flagNodeParent = selectedNode.userObject as FlagNodeParent
                val selection = StringSelection(flagNodeParent.key)
                val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
                return clipboard.setContents(selection, selection)
            } else {
                selectedNode = selectedNode.parent as? DefaultMutableTreeNode
            }
        }

        // If we can't find the key to copy, notify the user
        Notifier(project, Notifier.LDNotificationType.GENERAL).notify("Could not copy flag key.")
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = e.project ?: return
        val selectedNode = ActionHelpers.getLastSelectedDefaultMutableTreeNode(project) ?: return
        val isFlagParentNode = selectedNode.userObject is FlagNodeParent
        val hasKeyPrefix = selectedNode.toString().startsWith(KEY_PREFIX)

        e.presentation.isEnabledAndVisible = e.presentation.isEnabled && (hasKeyPrefix || isFlagParentNode)
    }
}
