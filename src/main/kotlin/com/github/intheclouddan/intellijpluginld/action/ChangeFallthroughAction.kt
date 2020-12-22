package com.github.intheclouddan.intellijpluginld.action

import com.github.intheclouddan.intellijpluginld.LaunchDarklyApiClient
import com.github.intheclouddan.intellijpluginld.settings.LaunchDarklyMergedSettings
import com.github.intheclouddan.intellijpluginld.toolwindow.FlagNodeParent
import com.github.intheclouddan.intellijpluginld.toolwindow.FlagToolWindow
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.launchdarkly.api.ApiException
import com.launchdarkly.api.model.PatchComment
import com.launchdarkly.api.model.PatchOperation
import com.launchdarkly.api.model.Variation
import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.Icon
import javax.swing.JList
import javax.swing.tree.DefaultMutableTreeNode

/**
 * Action class to demonstrate how to interact with the IntelliJ Platform.
 * The only action this class performs is to provide the user with a popup dialog as feedback.
 * Typically this class is instantiated by the IntelliJ Platform framework based on declarations
 * in the plugin.xml file. But when added at runtime this class is instantiated by an action group.
 */
class ChangeFallthroughAction : AnAction {
    /**
     * This default constructor is used by the IntelliJ Platform framework to
     * instantiate this class based on plugin.xml declarations. Only needed in PopupDialogAction
     * class because a second constructor is overridden.
     * @see AnAction.AnAction
     */
    constructor() : super()

    companion object {
        const val ID = "com.github.intheclouddan.intellijpluginld.action.ChangeFallthroughAction"
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
        val project = event.project!!
        val currentComponent = event?.inputEvent?.component ?: return
        val selectedNode = project.service<FlagToolWindow>().getPanel().tree.lastSelectedPathComponent as DefaultMutableTreeNode
        val parentNodeMut = selectedNode.parent as DefaultMutableTreeNode
        val parentNode = parentNodeMut.userObject as FlagNodeParent
        JBPopupFactory.getInstance().createPopupChooserBuilder(parentNode.flag.variations)
                .setTitle("New Fallthrough Variation")
                .setMovable(false).setResizable(false)
                .setRenderer(object : DefaultListCellRenderer() {
                    override fun getListCellRendererComponent(list: JList<*>,
                                                              value: Any?,
                                                              index: Int,
                                                              isSelected: Boolean,
                                                              cellHasFocus: Boolean): Component {
                        val rendererComponent = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                        val variation = value as Variation
                        text = "${variation.name ?: variation.value} ${if (variation.description != null) ": ${variation.description}" else ""}"
                        return rendererComponent
                    }
                })
                .setItemChosenCallback {
                    ApplicationManager.getApplication().executeOnPooledThread {
                        val settings = LaunchDarklyMergedSettings.getInstance(project)
                        val patchComment = PatchComment()
                        val patch = PatchOperation()
                        val currentIdx = parentNode.flag.variations.indexOf(it)
                        patch.op = "replace"
                        patch.path = "/environments/" + settings.environment + "/fallthrough/variation"
                        patch.value = currentIdx
                        patchComment.patch = listOf(patch)
                        val ldFlag = LaunchDarklyApiClient.flagInstance(project)
                        try {
                            ldFlag.patchFeatureFlag(settings.project, parentNode.key, patchComment)
                        } catch (e: ApiException) {
                            System.err.println("Exception when calling FeatureFlagsApi#patchFeatureFlag")
                            e.printStackTrace()
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
        val project = e.project
        if (project != null) {
            if (project.service<FlagToolWindow>().getPanel().tree.lastSelectedPathComponent != null) {
                val selectedNode = project.service<FlagToolWindow>().getPanel().tree.lastSelectedPathComponent.toString()
                e.presentation.isEnabledAndVisible = e.presentation.isEnabled && (selectedNode.startsWith("Fallthrough"))
            }
        }
    }
}