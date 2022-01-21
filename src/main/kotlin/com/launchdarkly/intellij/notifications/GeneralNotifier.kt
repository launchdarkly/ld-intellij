package com.launchdarkly.intellij.notifications

import com.intellij.notification.*
import com.intellij.openapi.project.Project

class GeneralNotifier {
    private val notificationGroup =
        NotificationGroupManager.getInstance().getNotificationGroup("LaunchDarkly")

    fun notify(project: Project?, content: String?): Notification {
        val notification: Notification = notificationGroup.createNotification(content!!, NotificationType.ERROR)
        notification.notify(project)
        return notification
    }
}