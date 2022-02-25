package com.launchdarkly.intellij.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.launchdarkly.api.ApiException
import com.launchdarkly.api.model.PatchComment
import com.launchdarkly.api.model.PatchOperation
import com.launchdarkly.api.model.Variation
import com.launchdarkly.intellij.LaunchDarklyApiClient
import com.launchdarkly.intellij.notifications.GeneralNotifier
import com.launchdarkly.intellij.settings.LaunchDarklyMergedSettings
import com.launchdarkly.intellij.toolwindow.FlagNodeParent
import com.launchdarkly.intellij.toolwindow.FlagToolWindow
import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.Icon
import javax.swing.JList
import javax.swing.tree.DefaultMutableTreeNode

/**
 * ChangeOffVariationAction allows users to update the Off targeting
 * for the selected flag in the configured environment.
 */
class ChangeOffVariationAction : AnAction {
    /**
     *  breaks if this is not called, even though IntelliJ says it's never used.
     */
    constructor() : super()

    companion object {
        const val ID = "com.launchdarkly.intellij.action.ChangeOffVariationAction"
    }

    /**
     * This constructor is used to support dynamically added menu actions.
     * It sets the text, description to be displayed for the menu item.
     * Otherwise, the default AnAction constructor is used by the IntelliJ Platform.
     * @param text  The text to be displayed as a menu item.
     * @param description  The description of the menu item.
     * @param icon  The icon to be used with the menu item.
     */
    constructor(text: String? = "Open in Browser", description: String?, icon: Icon?) : super(text, description, icon)

    /**
     * Gives the user feedback when the dynamic action menu is chosen.
     * Pops a simple message dialog. See the psi_demo plugin for an
     * example of how to use AnActionEvent to access data.
     * @param event Event received when the associated menu item is chosen.
     */
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project!!
        val currentComponent = event?.inputEvent?.component ?: return
        val selectedNode =
            project.service<FlagToolWindow>()
                .getPanel().getFlagPanel().tree.lastSelectedPathComponent as DefaultMutableTreeNode
        val parentNodeMut = selectedNode.parent as DefaultMutableTreeNode
        val parentNode = parentNodeMut.userObject as FlagNodeParent
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
                val settings = LaunchDarklyMergedSettings.getInstance(project)
                val patchComment = PatchComment()
                val patch = PatchOperation()
                val currentIdx = parentNode.flag.variations.indexOf(it)
                patch.op = "replace"
                patch.path = "/environments/" + settings.environment + "/offVariation"
                patch.value = currentIdx
                patchComment.patch = listOf(patch)
                val ldFlag = LaunchDarklyApiClient.flagInstance(project)
                ApplicationManager.getApplication().executeOnPooledThread {
                    try {
                        ldFlag.patchFeatureFlag(settings.project, parentNode.key, patchComment)
                    } catch (e: ApiException) {
                        System.err.println("Exception when calling FeatureFlagsApi#patchFeatureFlag")
                        e.printStackTrace()
                        val notifier = GeneralNotifier()
                        notifier.notify(
                            project,
                            "Error changing off variation for flag: ${parentNode.key} - ${e.message}"
                        )
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
        if (project.service<FlagToolWindow>().getPanel().getFlagPanel().tree.lastSelectedPathComponent != null) {
            val selectedNode =
                project.service<FlagToolWindow>().getPanel().getFlagPanel().tree.lastSelectedPathComponent.toString()
            e.presentation.isEnabledAndVisible =
                e.presentation.isEnabled && (selectedNode.startsWith("Off Variation:"))
        }
    }
}
