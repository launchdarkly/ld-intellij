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

/**
 * Action class to demonstrate how to interact with the IntelliJ Platform.
 * The only action this class performs is to provide the user with a popup dialog as feedback.
 * Typically this class is instantiated by the IntelliJ Platform framework based on declarations
 * in the plugin.xml file. But when added at runtime this class is instantiated by an action group.
 */
class ToggleFlagAction : AnAction {
    /**
     * This default constructor is used by the IntelliJ Platform framework to
     * instantiate this class based on plugin.xml declarations. Only needed in PopupDialogAction
     * class because a second constructor is overridden.
     * @see AnAction
     */
    constructor() : super()

    companion object {
        const val ID = "com.launchdarkly.intellij.action.ToggleFlagAction"
    }

    /**
     * This constructor is used to support dynamically added menu actions.
     * It sets the text, description to be displayed for the menu item.
     * Otherwise, the default AnAction constructor is used by the IntelliJ Platform.
     * @param text  The text to be displayed as a menu item.
     * @param description  The description of the menu item.
     * @param icon  The icon to be used with the menu item.
     */
    constructor(text: String?, description: String?, icon: Icon?) : super(text, description, icon)

    /**
     * Gives the user feedback when the dynamic action menu is chosen.
     * Pops a simple message dialog. See the psi_demo plugin for an
     * example of how to use AnActionEvent to access data.
     * @param event Event received when the associated menu item is chosen.
     */
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

    /**
     * Check that this is a top level flag node.
     * Requires a project to be open.
     * @param e Event received when the associated group-id menu is chosen.
     */
    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = e.project ?: return
        val selectedNode = ActionHelpers.getLastSelectedDefaultMutableTreeNode(project) ?: return
        val isFlagNode = selectedNode?.userObject is FlagNodeParent

        e.presentation.isEnabledAndVisible = e.presentation.isEnabled && isFlagNode
    }
}
