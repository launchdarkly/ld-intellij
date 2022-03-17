package com.launchdarkly.intellij.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.launchdarkly.api.ApiException
import com.launchdarkly.api.model.PatchComment
import com.launchdarkly.api.model.PatchOperation
import com.launchdarkly.api.model.Variation
import com.launchdarkly.intellij.LaunchDarklyApiClient
import com.launchdarkly.intellij.notifications.GeneralNotifier
import com.launchdarkly.intellij.settings.LaunchDarklyApplicationConfig
import com.launchdarkly.intellij.toolwindow.FlagNodeParent
import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.Icon
import javax.swing.JList
import javax.swing.tree.DefaultMutableTreeNode

/**
 * ChangeFallthroughAction allows users to update the Fallthrough targeting
 * for the selected flag in the configured environment.
 */
class ChangeFallthroughAction : AnAction() {
    companion object {
        const val ID = "com.launchdarkly.intellij.action.ChangeFallthroughAction"
    }

    /**
     * Parse the node this action is associated with and update the Fallthrough variation via API call.
     */
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val selectedNode = ActionHelpers.getLastSelectedDefaultMutableTreeNode(project) ?: return
        val parentNodeMut = selectedNode.parent as? DefaultMutableTreeNode ?: return
        val parentNode = parentNodeMut.userObject as? FlagNodeParent ?: return
        val currentComponent = event.inputEvent?.component ?: return

        JBPopupFactory.getInstance().createPopupChooserBuilder(parentNode.flag.variations)
            .setTitle("New Fallthrough Variation")
            .setMovable(false).setResizable(false)
            .setRenderer(object : DefaultListCellRenderer() {
                override fun getListCellRendererComponent(
                    list: JList<*>,
                    value: Any?,
                    index: Int,
                    isSelected: Boolean,
                    cellHasFocus: Boolean
                ): Component {
                    val rendererComponent =
                        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                    val variation = value as Variation
                    text =
                        "${variation.name ?: variation.value} ${if (variation.description != null) ": ${variation.description}" else ""}"
                    return rendererComponent
                }
            })
            .setItemChosenCallback {
                ApplicationManager.getApplication().executeOnPooledThread {
                    val settings = LaunchDarklyApplicationConfig.getInstance().ldState
                    val currentIdx = parentNode.flag.variations.indexOf(it)
                    val flagPatch = PatchOperation().apply {
                        op = "replace"
                        path = "/environments/" + settings.environment + "/fallthrough/variation"
                        value = currentIdx
                    }
                    val patchComment = PatchComment().apply {
                        patch = listOf(flagPatch)
                    }
                    val ldFlag = LaunchDarklyApiClient.flagInstance(project)
                    try {
                        ldFlag.patchFeatureFlag(settings.project, parentNode.key, patchComment)
                    } catch (e: ApiException) {
                        System.err.println("Exception when calling FeatureFlagsApi#patchFeatureFlag")
                        e.printStackTrace()
                        val notifier = GeneralNotifier()
                        notifier.notify(
                            project,
                            "Error changing fallthrough variation for flag: ${parentNode.key} - ${e.message}"
                        )
                    }
                }
            }
            .createPopup()
            .showUnderneathOf(currentComponent)
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = e.project ?: return
        val selectedNode = ActionHelpers.getLastSelectedDefaultMutableTreeNode(project) ?: return
        e.presentation.isEnabledAndVisible =
            e.presentation.isEnabled && (selectedNode.toString().startsWith("Fallthrough"))
    }
}
