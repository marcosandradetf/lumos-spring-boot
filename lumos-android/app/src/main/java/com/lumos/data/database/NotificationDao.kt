package com.lumos.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lumos.notifications.NotificationItem

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNotification(notification: NotificationItem)

    @Query("SELECT COUNT(id) FROM notifications_items")
    suspend fun getNotificationCount(): Int

    @Query("SELECT * FROM notifications_items ORDER BY ID DESC LIMIT 15")
    suspend fun getNotifications(): List<NotificationItem>

    @Query("DELETE FROM notifications_items WHERE persistCode is null")
    suspend fun deleteAllNonPersistent()

    @Query("DELETE FROM notifications_items where id = :id and persistCode is null")
    suspend fun deleteNonPersistentById(id: Long)

    @Query("DELETE FROM notifications_items where persistCode = :persistCode")
    suspend fun deletePersistentNotification(persistCode: String)

}