package com.github.intheclouddan.intellijpluginld.notifications

import com.intellij.notification.Notification
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

class ConfigNotifier {
    private val NOTIFICATION_GROUP = NotificationGroup("Launchdarkly Configuration", NotificationDisplayType.BALLOON, true)
//    fun notify(content: String?): Notification {
//        return notify(null, content)
//    }

    fun notify(project: Project?, content: String?): Notification {
        val notification: Notification = NOTIFICATION_GROUP.createNotification(content!!, NotificationType.ERROR)
        notification.notify(project)
        return notification
    }
}