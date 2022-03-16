package com.launchdarkly.intellij.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.launchdarkly.intellij.toolwindow.FlagNodeParent
import com.launchdarkly.intellij.toolwindow.FlagToolWindow
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection
import javax.swing.Icon
import javax.swing.tree.DefaultMutableTreeNode

const val FLAG_NAME_DEPTH = 1
const val FLAG_NAME_PATH = 2

/**
 * Action class to demonstrate how to interact with the IntelliJ Platform.
 * The only action this class performs is to provide the user with a popup dialog as feedback.
 * Typically this class is instantiated by the IntelliJ Platform framework based on declarations
 * in the plugin.xml file. But when added at runtime this class is instantiated by an action group.
 */
class CopyKeyAction : AnAction {
    constructor() : super()

    companion object {
        const val ID = "com.launchdarkly.intellij.action.CopyKeyAction"
    }

    /**
     * This constructor is used to support dynamically added menu actions.
     * It sets the text, description to be displayed for the menu item.
     * Otherwise, the default AnAction constructor is used by the IntelliJ Platform.
     * @param text  The text to be displayed as a menu item.
     * @param description  The description of the menu item.
     * @param icon  The icon to be used with the menu item.
     */
    constructor(text: String? = "Copy Key", description: String?, icon: Icon?) : super(text, description, icon)

    /**
     * Gives the user feedback when the dynamic action menu is chosen.
     * Pops a simple message dialog. See the psi_demo plugin for an
     * example of how to use AnActionEvent to access data.
     * @param event Event received when the associated menu item is chosen.
     */
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
        var selection = StringSelection("")
        if (project != null) {
            val selectedNode = ActionHelpers.getLastSelectedPathComponent(project)
            if (selectedNode != null) {
                // Right clicking on Key node. Will break if order changes.
                if (selectedNode.childCount == 0 && selectedNode.toString().startsWith("Key:")) {
                    selection = StringSelection(selectedNode.toString().substringAfter(" "))
                } else if (selectedNode.depth == FLAG_NAME_DEPTH) { //
                    selection = StringSelection(selectedNode.firstChild.toString().substringAfter(" "))
                }
                val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
                clipboard.setContents(selection, selection)
            }
        }
    }

    /**
     * Determines whether this menu item is available for the current context.
     * Requires a project to be open.
     * @param e Event received when the associated group-id menu is chosen.
     */
    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = e.project ?: return
        val selectedNode = ActionHelpers.getLastSelectedPathComponent(project) ?: return
        val isFlagParentNode = selectedNode?.userObject as? FlagNodeParent

        e.presentation.isEnabledAndVisible =
            e.presentation.isEnabled && (selectedNode.toString().startsWith("Key:") || isFlagParentNode != null)

    }
}
