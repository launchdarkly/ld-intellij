package com.launchdarkly.intellij.toolwindow

import com.intellij.openapi.project.Project
import com.launchdarkly.intellij.notifications.GeneralNotifier

object Utilities {
    fun handlePanelError(err: Error, project: Project) {
        System.err.println("Exception when updating LaunchDarkly FlagPanel Toolwindow")
        err.printStackTrace()
        val notifier = GeneralNotifier()
        notifier.notify(
            project,
            "Error updating LaunchDarkly Toolwindow $err"
        )
    }
}