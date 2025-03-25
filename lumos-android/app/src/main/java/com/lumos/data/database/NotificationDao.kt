package com.lumos.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lumos.service.NotificationItem

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNotification(notification: NotificationItem)

    @Query("SELECT COUNT(id) FROM notifications_items")
    suspend fun getNotificationCount(): Int

    @Query("SELECT * FROM notifications_items")
    suspend fun getNotifications(): List<NotificationItem>

    @Query("DELETE FROM notifications_items")
    suspend fun deleteAll()

    @Query("DELETE FROM notifications_items where id = :id")
    suspend fun delete(id: Long)

}