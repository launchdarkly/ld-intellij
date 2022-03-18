package com.launchdarkly.intellij.notifications

import com.intellij.notification.*
import com.intellij.openapi.project.Project

class Notifier(private val project: Project) {
    private enum class LDNotificationType(val groupString: String) {
        CONFIG("LaunchDarkly Config"),
        GENERAL("LaunchDarkly"),
    }

    private lateinit var type: LDNotificationType

    companion object {
        fun createGeneralNotifier(project: Project): Notifier {
            val notifier = Notifier(project)
            notifier.type = LDNotificationType.GENERAL
            return notifier
        }

        fun createConfigNotifier(project: Project): Notifier {
            val notifier = Notifier(project)
            notifier.type = LDNotificationType.CONFIG
            return notifier
        }
    }

    fun notify(content: String): Notification {
        val notificationGroup = NotificationGroupManager.getInstance().getNotificationGroup(type.groupString)
        val notification: Notification = notificationGroup.createNotification(content, NotificationType.ERROR)
        notification.notify(project)
        return notification
    }
}
