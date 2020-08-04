package com.github.intheclouddan.intellijpluginld.action

import com.github.intheclouddan.intellijpluginld.LDIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DefaultActionGroup

/**
 * Creates an action group to contain menu actions. See plugin.xml declarations.
 */
class CustomDefaultActionGroup : DefaultActionGroup() {
    /**
     * Given CustomDefaultActionGroup is derived from ActionGroup, in this context
     * update() determines whether the action group itself should be enabled or disabled.
     * Requires an editor to be active in order to enable the group functionality.
     * @see com.intellij.openapi.actionSystem.AnAction.update
     * @param event  Event received when the associated group-id menu is chosen.
     */
    override fun update(event: AnActionEvent) {
        // Enable/disable depending on whether user is editing
        val editor = event.getData(CommonDataKeys.EDITOR)
        event.presentation.isEnabled = editor != null
        // Take this opportunity to set an icon for the menu entry.
        event.presentation.setIcon(LDIcons.DESCRIPTION)
    }
}