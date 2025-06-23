package com.lumos.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.lumos.data.database.AppDatabase
import com.lumos.domain.model.SyncQueueEntity
import java.util.concurrent.TimeUnit

object SyncManager {

    fun enqueueSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()

        val request = OneTimeWorkRequestBuilder<SyncQueueWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                15, TimeUnit.MINUTES
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "SyncQueueWorker",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun schedulePeriodicSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false) // roda mesmo com bateria baixa
            .build()

        val request = PeriodicWorkRequestBuilder<SyncQueueWorker>(
            6, TimeUnit.HOURS // ajustável: 6h, 12h, 24h...
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                15, TimeUnit.MINUTES
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "PeriodicSyncQueueWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    suspend fun queuePostPreMeasurement(context: Context, db: AppDatabase, contractId: Long) {
        val count = db.queueDao().countPendingItemsByTypeAndId(SyncTypes.POST_PRE_MEASUREMENT, contractId)
        if (count == 0) {
            val syncItem = SyncQueueEntity(
                relatedId = contractId,
                type = SyncTypes.POST_PRE_MEASUREMENT,
                priority = 30 // mais prioritário
            )
            db.queueDao().insert(syncItem)
            enqueueSync(context)
        }
    }

    suspend fun queueSyncContractItems(context: Context, db: AppDatabase) {
        val count = db.queueDao().countPendingItemsByType(SyncTypes.SYNC_CONTRACT_ITEMS)
        if (count == 0) {
            val syncItem = SyncQueueEntity(
                type = SyncTypes.SYNC_CONTRACT_ITEMS,
                priority = 40
            )
            db.queueDao().insert(syncItem)
            enqueueSync(context)
        }
    }

    suspend fun queueSyncContracts(context: Context, db: AppDatabase) {
        val count = db.queueDao().countPendingItemsByType(SyncTypes.SYNC_CONTRACTS)
        if (count == 0) {
            val syncItem = SyncQueueEntity(
                type = SyncTypes.SYNC_CONTRACTS,
                priority = 50
            )
            db.queueDao().insert(syncItem)
            enqueueSync(context)
        }
    }

    suspend fun queueSyncExecutions(context: Context, db: AppDatabase) {
        val count = db.queueDao().countPendingItemsByType(SyncTypes.SYNC_EXECUTIONS)
        if (count == 0) {
            val syncItem = SyncQueueEntity(
                type = SyncTypes.SYNC_EXECUTIONS,
                priority = 20
            )
            db.queueDao().insert(syncItem)
            enqueueSync(context)
        }
    }

    suspend fun queueSyncPostGeneric(
        context: Context,
        db: AppDatabase,
        table: String,
        field: String,
        set: String,
        where: String,
        equal: String,
    ) {
        val count = db.queueDao().countPendingGeneric(
            table, field, set, where, equal
        )
        if (count == 0) {
            val syncItem = SyncQueueEntity(
                type = SyncTypes.POST_GENERIC,
                priority = 90,
                table = table,
                field = field,
                set = set,
                where = where,
                equal = equal,
            )
            db.queueDao().insert(syncItem)
            enqueueSync(context)
        }
    }

    suspend fun queuePostIndirectExecution(context: Context, db: AppDatabase, streetId: Long) {
        val count = db.queueDao().countPendingItemsByTypeAndId(SyncTypes.POST_INDIRECT_EXECUTION, streetId)
        if (count == 0) {

            val syncItem = SyncQueueEntity(
                relatedId = streetId,
                type = SyncTypes.POST_INDIRECT_EXECUTION,
                priority = 19
            )
            db.queueDao().insert(syncItem)
            enqueueSync(context)
        }

    }

    suspend fun queuePostDirectExecution(context: Context, db: AppDatabase, streetId: Long) {
        val count = db.queueDao().countPendingItemsByTypeAndId(SyncTypes.POST_DIRECT_EXECUTION, streetId)
        if (count == 0) {

            val syncItem = SyncQueueEntity(
                relatedId = streetId,
                type = SyncTypes.POST_DIRECT_EXECUTION,
                priority = 18
            )
            db.queueDao().insert(syncItem)
            enqueueSync(context)
        }
    }

}
