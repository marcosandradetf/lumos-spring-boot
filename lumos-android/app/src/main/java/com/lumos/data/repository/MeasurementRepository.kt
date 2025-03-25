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
import com.lumos.data.api.PreMeasurementDto
import com.lumos.data.api.PreMeasurementStreetDto
import com.lumos.data.database.PreMeasurementDao
import com.lumos.domain.model.PreMeasurement
import com.lumos.domain.model.PreMeasurementStreetItem
import com.lumos.domain.model.PreMeasurementStreet
import com.lumos.domain.service.SyncMeasurement
import java.util.concurrent.TimeUnit

class MeasurementRepository(
    private val dao: PreMeasurementDao,
    private val api: MeasurementApi,
    private val context: Context
) {


    suspend fun saveStreet(preMeasurementStreet: PreMeasurementStreet): Long? {
        return try {
            dao.insertStreet(preMeasurementStreet)
        } catch (e: Exception) {
            Log.e("Error saveMeasurement", e.message.toString())
            null
        }
    }

    suspend fun getUnSyncedMeasurements(): List<PreMeasurement> {
        return dao.getUnSyncedPreMeasurements()
    }

    suspend fun sendMeasurementToBackend(
        preMeasurement: PreMeasurement,
        preMeasurementStreet: List<PreMeasurementStreet>,
        preMeasurementStreetItems: List<PreMeasurementStreetItem>,
        userUuid: String
    ): Boolean {
        return try {
            val streets: MutableList<PreMeasurementStreetDto> = mutableListOf()
            val itemsByStreetId = preMeasurementStreetItems.groupBy { it.preMeasurementStreetId }

            preMeasurementStreet.forEach { street ->
                val items = itemsByStreetId[street.preMeasurementStreetId] ?: emptyList()
                streets.add(
                    PreMeasurementStreetDto(
                        street = street,
                        items = items
                    )
                )
            }

            val dto = PreMeasurementDto(
                contractId = preMeasurement.contractID,
                streets = streets
            )
            api.sendMeasurement(dto, userUuid)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun markAsSynced(id: Long) {
        dao.markAsSynced(id)
    }

    suspend fun saveItem(preMeasurementStreetItem: PreMeasurementStreetItem) {
        dao.insertItem(preMeasurementStreetItem)
    }

    suspend fun getItems(measurementId: Long): List<PreMeasurementStreetItem> {
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
                    .setRequiresBatteryNotLow(false)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "sync_measurements", // Nome único para o trabalho
            ExistingWorkPolicy.REPLACE, // Pode substituir o trabalho se já estiver agendado
            workRequest
        )
    }

    suspend fun savePreMeasurement(preMeasurement: PreMeasurement): Long {
        return dao.insertPreMeasurement(preMeasurement)
    }

    suspend fun getPreMeasurements(status: String): List<PreMeasurement> {
        return dao.getPreMeasurements(status)
    }

    suspend fun getPreMeasurement(preMeasurementId: Long): PreMeasurement {
        return dao.getPreMeasurement(preMeasurementId)
    }

    suspend fun getStreets(preMeasurementId: Long): List<PreMeasurementStreet> {
        return dao.getStreets(preMeasurementId)
    }

}

object Status {
    const val PENDING = "PEDING"
    const val IN_PROGRESS = "IN_PROGRESS"
    const val FINISHED = "FINISHED"
}