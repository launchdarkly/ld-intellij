package com.launchdarkly.intellij.toolwindow

import com.intellij.openapi.project.Project
import com.launchdarkly.intellij.notifications.Notifier

object Utilities {
    fun handlePanelError(err: Error, project: Project) {
        System.err.println("Exception when updating LaunchDarkly FlagPanel Toolwindow")
        err.printStackTrace()
        Notifier(project, Notifier.LDNotificationType.GENERAL).notify("Error updating LaunchDarkly Toolwindow $err")
    }
}
