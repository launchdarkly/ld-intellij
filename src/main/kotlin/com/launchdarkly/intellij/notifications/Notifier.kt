package com.launchdarkly.intellij.notifications

import com.intellij.notification.*
import com.intellij.openapi.project.Project

class Notifier(project: Project, type: LDNotificationType) {
    enum class LDNotificationType(val groupString: String) {
        CONFIG("LaunchDarkly Config"),
        GENERAL("LaunchDarkly"),
    }

    val project: Project
    val type: LDNotificationType

    init {
        this.project = project
        this.type = type
    }

    fun notify(content: String): Notification {
        val notificationGroup = NotificationGroupManager.getInstance().getNotificationGroup(type.groupString)
        val notification: Notification = notificationGroup.createNotification(content, NotificationType.ERROR)
        notification.notify(project)
        return notification
    }
}
