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
import com.launchdarkly.intellij.notifications.Notifier
import com.launchdarkly.intellij.settings.LaunchDarklyApplicationConfig
import com.launchdarkly.intellij.toolwindow.FlagNodeParent
import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.JList
import javax.swing.tree.DefaultMutableTreeNode

/**
 * ChangeOffVariationAction allows users to update the Off targeting
 * for the selected flag in the configured environment.
 */
class ChangeOffVariationAction : AnAction() {
    companion object {
        const val ID = "com.launchdarkly.intellij.action.ChangeOffVariationAction"
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val currentComponent = event.inputEvent?.component ?: return
        val selectedNode = ActionHelpers.getLastSelectedDefaultMutableTreeNode(project) ?: return
        val parentNodeMut = selectedNode.parent as? DefaultMutableTreeNode ?: return
        val parentNode = parentNodeMut.userObject as? FlagNodeParent ?: return

        JBPopupFactory.getInstance().createPopupChooserBuilder(parentNode.flag.variations)
            .setTitle("New Off Variation")
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
                val settings = LaunchDarklyApplicationConfig.getInstance().ldState
                val patchComment = PatchComment()
                val patch = PatchOperation()
                val currentIdx = parentNode.flag.variations.indexOf(it)
                patch.op = "replace"
                patch.path = "/environments/" + settings.environment + "/offVariation"
                patch.value = currentIdx
                patchComment.patch = listOf(patch)
                val ldFlag = LaunchDarklyApiClient.flagInstance()
                ApplicationManager.getApplication().executeOnPooledThread {
                    try {
                        ldFlag.patchFeatureFlag(settings.project, parentNode.key, patchComment)
                    } catch (e: ApiException) {
                        System.err.println("Exception when calling FeatureFlagsApi#patchFeatureFlag")
                        e.printStackTrace()
                        Notifier(project, Notifier.LDNotificationType.GENERAL).notify("Error changing off variation for flag: ${parentNode.key} - ${e.message}")
                    }
                }
            }
            .createPopup()
            .showUnderneathOf(currentComponent)
    }

    /**
     * Determines whether this menu item is available for the current context.
     * Requires a project to be open.
     * @param e Event received when the associated group-id menu is chosen.
     */
    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = e.project ?: return
        val selectedNode = ActionHelpers.getLastSelectedDefaultMutableTreeNode(project) ?: return

        e.presentation.isEnabledAndVisible =
            e.presentation.isEnabled && (selectedNode.toString().startsWith("Off Variation:"))
    }
}
