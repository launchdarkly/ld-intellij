package com.launchdarkly.intellij.action

import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.launchdarkly.intellij.settings.LaunchDarklyMergedSettings
import com.launchdarkly.intellij.toolwindow.FlagNodeParent
import com.launchdarkly.intellij.toolwindow.FlagToolWindow
import com.launchdarkly.intellij.notifications.GeneralNotifier
import javax.swing.Icon
import javax.swing.tree.DefaultMutableTreeNode

/**
 * Action class to demonstrate how to interact with the IntelliJ Platform.
 * The only action this class performs is to provide the user with a popup dialog as feedback.
 * Typically this class is instantiated by the IntelliJ Platform framework based on declarations
 * in the plugin.xml file. But when added at runtime this class is instantiated by an action group.
 */
class OpenInBrowserAction : AnAction {
    /**
     * This default constructor is used by the IntelliJ Platform framework to
     * instantiate this class based on plugin.xml declarations. Only needed in PopupDialogAction
     * class because a second constructor is overridden.
     * @see AnAction.AnAction
     */
    constructor() : super()

    companion object {
        const val ID = "com.launchdarkly.intellij.action.OpenInBrowserAction"
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
        val project = event.project
        val settings = LaunchDarklyMergedSettings.getInstance(project!!)
        if (project.service<FlagToolWindow>().getPanel().tree.lastSelectedPathComponent !== null) {
            val selectedNode =
                project.service<FlagToolWindow>().getPanel().tree.lastSelectedPathComponent as DefaultMutableTreeNode
            val nodeInfo: FlagNodeParent = selectedNode.userObject as FlagNodeParent
            if (nodeInfo != null) {
                val url = "${settings.baseUri}/${settings.project}/${settings.environment}/features/${nodeInfo.flag.key}"
                BrowserLauncher.instance.open(url)
            }
        } else {
            val notifier = GeneralNotifier()
            notifier.notify(project, "Error opening in browser, please try again.")
        }

    }

    /**
     * Determines whether this menu item is available for the current context.
     * Requires a project to be open.
     * @param e Event received when the associated group-id menu is chosen.
     */
    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = e.project
        if (project != null) {
            if (project.service<FlagToolWindow>().getPanel().tree.lastSelectedPathComponent != null) {
                val selectedNode =
                    project.service<FlagToolWindow>().getPanel().tree.lastSelectedPathComponent.toString()
                e.presentation.isEnabledAndVisible = e.presentation.isEnabled && project.service<FlagToolWindow>()
                    .getPanel().tree.selectionPath.path.size == FLAG_NAME_PATH
            }
        }
    }
}