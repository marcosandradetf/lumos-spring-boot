package com.lumos.data.repository

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.lumos.data.api.MeasurementApi
import com.lumos.data.api.MeasurementDto
import com.lumos.data.database.MeasurementDao
import com.lumos.domain.model.Item
import com.lumos.domain.model.Measurement
import com.lumos.domain.service.SyncMeasurement
import java.util.concurrent.TimeUnit

class MeasurementRepository(
    private val dao: MeasurementDao,
    private val api: MeasurementApi,
    private val context: Context
) {


    suspend fun saveMeasurement(measurement: Measurement): Long? {
        return try {
            dao.insertMeasurement(measurement)
        } catch (e: Exception) {
            Log.e("Error saveMeasurement", e.message.toString())
            null
        }
    }

    suspend fun getUnsyncedMeasurements(): List<Measurement> {
        return dao.getUnsyncedMeasurements()
    }

    suspend fun sendMeasurementToBackend(measurement: Measurement, items: List<Item>): Boolean {
        return try {
            val dto = MeasurementDto(
                measurement,
                items
            )
            api.sendMeasurement(dto)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun markAsSynced(id: Long) {
        dao.markAsSynced(id)
    }

    suspend fun saveItem(item: Item) {
        dao.insertItem(item)
    }

    suspend fun getItems(measurementId: Long): List<Item> {
        return dao.getItems(measurementId)
    }

    fun syncMeasurement() {
        // Agendar o Worker assim que a medição for adicionada
        val workRequest = OneTimeWorkRequestBuilder<SyncMeasurement>()
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30, TimeUnit.MINUTES
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "sync_measurements", // Nome único para o trabalho
            ExistingWorkPolicy.REPLACE, // Pode substituir o trabalho se já estiver agendado
            workRequest
        )
    }

}