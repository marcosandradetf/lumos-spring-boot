package com.lumos.data.repository

import com.lumos.data.entities.Measurement
import com.lumos.service.ApiService

class MeasurementRepository(
    private val dao: MeasurementDao,
    private val api: ApiService
) {
    suspend fun saveMeasurement(measurement: Measurement) {
        dao.insert(measurement)
    }

    suspend fun getUnsyncedMeasurements(): List<Measurement> = dao.getUnsyncedMeasurements()

    suspend fun sendMeasurementToBackend(measurement: Measurement): Boolean {
        return try {
            api.sendMeasurement(measurement)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun markAsSynced(id: Long) {
        dao.markAsSynced(id)
    }
}