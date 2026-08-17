package dev.containermanager.applecontainer.util

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

object AppleContainerNotifier {
    private const val GROUP_ID = "Apple Container Manager"

    fun info(project: Project?, title: String, content: String = "") =
        notify(project, title, content, NotificationType.INFORMATION)

    fun warn(project: Project?, title: String, content: String = "") =
        notify(project, title, content, NotificationType.WARNING)

    fun error(project: Project?, title: String, content: String = "") =
        notify(project, title, content, NotificationType.ERROR)

    private fun notify(project: Project?, title: String, content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(GROUP_ID)
            .createNotification(title, content, type)
            .notify(project)
    }
}
