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

    @Query("SELECT * FROM sync_queue_entity WHERE status IN (:statuses) ORDER BY priority ASC, createdAt ASC")
    suspend fun getItemsByStatuses(statuses: List<String>): List<SyncQueueEntity>

}
