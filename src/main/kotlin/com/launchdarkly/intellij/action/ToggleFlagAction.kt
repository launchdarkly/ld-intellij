package com.launchdarkly.intellij.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.launchdarkly.api.ApiException
import com.launchdarkly.api.model.PatchComment
import com.launchdarkly.api.model.PatchOperation
import com.launchdarkly.intellij.LaunchDarklyApiClient
import com.launchdarkly.intellij.notifications.GeneralNotifier
import com.launchdarkly.intellij.settings.LaunchDarklyApplicationConfig
import com.launchdarkly.intellij.toolwindow.FlagNodeParent
import javax.swing.Icon

class ToggleFlagAction : AnAction() {
    companion object {
        const val ID = "com.launchdarkly.intellij.action.ToggleFlagAction"
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val selectedNode = ActionHelpers.getLastSelectedDefaultMutableTreeNode(project) ?: return
        val flagNode = selectedNode.userObject as? FlagNodeParent ?: return

        val settings = LaunchDarklyApplicationConfig.getInstance().ldState
        val flagPatch = PatchOperation().apply {
            op = "replace"
            path = "/environments/" + settings.environment + "/on"
            value = !flagNode.env.on
        }
        val patchComment = PatchComment().apply {
            patch = listOf(flagPatch)
        }
        val ldFlag = LaunchDarklyApiClient.flagInstance(project)
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                ldFlag.patchFeatureFlag(settings.project, flagNode.key, patchComment)
            } catch (e: ApiException) {
                System.err.println("Exception when calling FeatureFlagsApi#patchFeatureFlag")
                e.printStackTrace()
                val notifier = GeneralNotifier()
                notifier.notify(project, "Error toggling flag: $flagNode.key - ${e.message}")
            }
        }
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = e.project ?: return
        val selectedNode = ActionHelpers.getLastSelectedDefaultMutableTreeNode(project) ?: return
        val isFlagNode = selectedNode.userObject is FlagNodeParent

        e.presentation.isEnabledAndVisible = e.presentation.isEnabled && isFlagNode
    }
}
