package com.lumos.repository

import android.app.Application
import com.lumos.data.database.AppDatabase
import com.lumos.data.database.NotificationDao
import com.lumos.midleware.SecureStorage
import com.lumos.notifications.NotificationItem
import com.lumos.worker.SyncManager

class NotificationRepository(
    private val db: AppDatabase,
    private val app: Application
) {
    private val secureStorage = SecureStorage(app.applicationContext)

    suspend fun getAll(): List<NotificationItem> {
        return db.notificationDao().getNotifications()
    }

    suspend fun delete(id: Long) {
        db.notificationDao().deleteNonPersistentById(id)
    }

    suspend fun deleteAll() {
        db.notificationDao().deleteAllNonPersistent()
    }

    suspend fun insert(notificationItem: NotificationItem): Int {
        db.notificationDao().insertNotification(notificationItem)
        return db.notificationDao().getNotificationCount()
    }

    suspend fun countNotifications(): Int {
        return db.notificationDao().getNotificationCount()
    }

    suspend fun changeTeam(teamId: String?) {
        try {
            teamId?.let {
                secureStorage.setTeamId(it.toLong())
                db.stockDao().deleteStock()
                SyncManager.queueGetStock(app.applicationContext, db)
            }
        } catch (e: Exception) {
            throw e
        }
    }

}