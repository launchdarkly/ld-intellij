package com.github.intheclouddan.intellijpluginld.notifications

import com.intellij.notification.Notification
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

class ConfigNotifier {
    private val notificationGroup = NotificationGroup("Launchdarkly Configuration", NotificationDisplayType.BALLOON, true)

    fun notify(project: Project?, content: String?): Notification {
        val notification: Notification = notificationGroup.createNotification(content!!, NotificationType.ERROR)
        notification.notify(project)
        return notification
    }
}