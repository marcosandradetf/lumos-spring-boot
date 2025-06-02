package com.lumos.data.repository

import com.lumos.data.database.NotificationDao
import com.lumos.notifications.NotificationItem

class NotificationRepository(
    private val dao: NotificationDao,
) {

    suspend fun getAll() : List<NotificationItem> {
        return dao.getNotifications()
    }

    suspend fun delete(id: Long) {
        dao.deleteNonPersistentById(id)
    }

    suspend fun deleteAll() {
        dao.deleteAllNonPersistent()
    }

    suspend fun insert(notificationItem: NotificationItem) : Int {
        dao.insertNotification(notificationItem)
        return dao.getNotificationCount()
    }

    suspend fun countNotifications(): Int {
        return dao.getNotificationCount()
    }

}