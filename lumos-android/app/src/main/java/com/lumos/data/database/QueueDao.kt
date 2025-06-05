package com.lumos.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lumos.domain.model.SyncQueueEntity
import com.lumos.worker.SyncStatus

@Dao
interface QueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncQueueEntity): Long

    @Query(
        """
            SELECT * FROM sync_queue_entity
            WHERE (status = :pending OR status = :inProgress)
               OR (status = :failed AND attemptCount <= :maxAttempts)
               ORDER BY priority ASC, createdAt ASC
        """
    )
    suspend fun getItemsToProcess(
        pending: String = SyncStatus.PENDING,
        inProgress: String = SyncStatus.IN_PROGRESS,
        failed: String = SyncStatus.FAILED,
        maxAttempts: Int = 5
    ): List<SyncQueueEntity>

    @Update
    suspend fun update(item: SyncQueueEntity)

    @Query("DELETE FROM sync_queue_entity WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(type) FROM sync_queue_entity WHERE type = :type")
    suspend fun countPendingItemsByType(type: String): Int


    @Query("SELECT COUNT(relatedId) FROM sync_queue_entity WHERE type = :type and relatedId = :id")
    suspend fun countPendingItemsByTypeAndId(type: String, id: Long): Int

    @Query("SELECT COUNT(relatedId) FROM sync_queue_entity WHERE `table` = :table and field = :field and `set` = :set and `where` = :where and equal = :equal")
    suspend fun countPendingGeneric(
        table: String,
        field: String,
        set: String,
        where: String,
        equal: String
    ): Int

}
