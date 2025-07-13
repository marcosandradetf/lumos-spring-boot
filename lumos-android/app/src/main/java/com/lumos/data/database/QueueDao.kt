package com.lumos.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lumos.domain.model.SyncQueueEntity
import com.lumos.worker.SyncStatus
import kotlinx.coroutines.flow.Flow

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

    @Query(
        """
            SELECT * FROM sync_queue_entity
            WHERE type in (:types)
        """
    )
    suspend fun getItem(
        types: List<String>,
    ): List<SyncQueueEntity>

    @Update
    suspend fun update(item: SyncQueueEntity)

    @Query("DELETE FROM sync_queue_entity WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(type) FROM sync_queue_entity WHERE type = :type")
    suspend fun countPendingItemsByType(type: String): Int


    @Query("""
    SELECT COUNT(*) FROM sync_queue_entity 
    WHERE type = :type AND (relatedId = :id OR relatedUuid = :uuid)
""")
    suspend fun countPendingItemsByTypeAndId(
        type: String,
        id: Long? = null,
        uuid: String? = null
    ): Int

    @Query("SELECT COUNT(relatedId) FROM sync_queue_entity WHERE `table` = :table and field = :field and `set` = :set and `where` = :where and equal = :equal")
    suspend fun countPendingGeneric(
        table: String,
        field: String,
        set: String,
        where: String,
        equal: String
    ): Int

    @Query(
        """
            SELECT distinct type FROM sync_queue_entity
            where status = :status or errorMessage is not null
            ORDER BY priority ASC, createdAt ASC
        """
    )
    fun getFlowItemsToProcess(status: String = SyncStatus.FAILED): Flow<List<String>>

    @Query(
        """
        update sync_queue_entity
        set status = :status
        where relatedId = :relatedId and type = :type
    """
    )
    suspend fun retry(
        relatedId: Long,
        type: String,
        status: String = SyncStatus.PENDING
    )

    @Query(
        """
            SELECT EXISTS(
                SELECT 1 FROM sync_queue_entity
                WHERE relatedId = :relatedId AND type = :type
                LIMIT 1
            )
        """
    )
    suspend fun exists(
        relatedId: Long,
        type: String,
    ): Boolean

    @Query("DELETE FROM sync_queue_entity WHERE relatedId = :id and type = :type")
    suspend fun deleteByRelatedId(id: Long, type: String)

    @Query(
        """
            SELECT EXISTS(
                SELECT 1 FROM sync_queue_entity
                WHERE type in (:types)
                LIMIT 1
            )
        """
    )
    fun getFlowExistsTypeInQueue(types: List<String>): Flow<Boolean>


    @Query(
        """
        update sync_queue_entity
        set status = :status
        where relatedId = :id
    """
    )
    suspend fun retryById(
        id: Long,
        status: String = SyncStatus.PENDING
    )

    @Query(
        """
            SELECT EXISTS(
                SELECT 1 FROM sync_queue_entity
                WHERE id = :id
                LIMIT 1
            )
        """
    )
    suspend fun existsById(
        id: Long,
    ): Boolean


}
