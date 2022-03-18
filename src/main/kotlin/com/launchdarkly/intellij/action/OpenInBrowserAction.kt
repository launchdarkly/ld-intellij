package com.launchdarkly.intellij.action

import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.launchdarkly.intellij.notifications.Notifier
import com.launchdarkly.intellij.settings.LaunchDarklyApplicationConfig
import com.launchdarkly.intellij.toolwindow.FlagNodeParent
import com.launchdarkly.intellij.toolwindow.InfoNode

class OpenInBrowserAction : AnAction() {
    companion object {
        const val ID = "com.launchdarkly.intellij.action.OpenInBrowserAction"
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val settings = LaunchDarklyApplicationConfig.getInstance().ldState
        val selectedNode = ActionHelpers.getLastSelectedDefaultMutableTreeNode(project)

        if (selectedNode?.userObject is FlagNodeParent) {
            val flagParentNode = selectedNode.userObject as FlagNodeParent
            val url =
                "${settings.baseUri}/${settings.project}/${settings.environment}/features/${flagParentNode.flag.key}"
            BrowserLauncher.instance.open(url)
        } else if (selectedNode?.userObject is InfoNode) {
            val url = "${settings.baseUri}/${settings.project}/${settings.environment}/features"
            BrowserLauncher.instance.open(url)
        } else {
            Notifier(project, Notifier.LDNotificationType.GENERAL).notify("Error opening in browser, please try again.")
        }
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = e.project ?: return
        val selectedNode = ActionHelpers.getLastSelectedDefaultMutableTreeNode(project) ?: return
        val isFlagNode = selectedNode.userObject is FlagNodeParent
        val isInfoNode = selectedNode.userObject is InfoNode

        e.presentation.isEnabledAndVisible = e.presentation.isEnabled && (isFlagNode || isInfoNode)
    }
}
