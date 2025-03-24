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

    @Query("SELECT COUNT(id) FROM notificationsItems")
    suspend fun getNotificationCount(): Int

    @Query("SELECT * FROM notificationsItems")
    suspend fun getNotifications(): List<NotificationItem>

    @Query("DELETE FROM notificationsItems")
    suspend fun deleteAll()

    @Query("DELETE FROM notificationsItems where id = :id")
    suspend fun delete(id: Long)

}