package com.lumos.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lumos.worker.SyncStatus

@Entity(tableName = "sync_queue_entity")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val relatedId: Long? = null,
    val type: String, // "CONTRACT", "STOCK", "PRE_MEASUREMENT"
    val status: String = SyncStatus.PENDING,
    val priority: Int = 100, // Quanto menor, mais prioritário
    val createdAt: Long = System.currentTimeMillis(),
    val attemptCount: Int = 0,

    val table: String? = null,
    val field: String? = null,
    val set: String? = null,
    val where: String? = null,
    val equal: String? = null,
)