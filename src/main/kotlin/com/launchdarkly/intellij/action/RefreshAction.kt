package com.launchdarkly.intellij.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.launchdarkly.intellij.FlagStore
import javax.swing.Icon

class RefreshAction : AnAction() {
    companion object {
        const val ID = "com.launchdarkly.intellij.action.RefreshAction"
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return

        val flags = project.service<FlagStore>()
        flags.flagsNotify(reinit = true, rebuild = true)
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = e.project ?: return

        e.presentation.isEnabledAndVisible = true
    }
}
