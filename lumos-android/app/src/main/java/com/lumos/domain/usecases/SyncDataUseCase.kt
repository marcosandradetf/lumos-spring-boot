package com.lumos.domain.usecases


import android.content.Context
import com.lumos.data.entities.Measurement
import com.lumos.data.repository.MeasurementRepository
import com.lumos.utils.ConnectivityUtils


class SyncDataUseCase(
    private val context: Context,
    private val repository: MeasurementRepository
) {
    suspend fun execute() {
        if (ConnectivityUtils.isConnectedToInternet(context)) {
            val unsyncedData = repository.getUnsyncedMeasurements()
            unsyncedData.forEach { measurement ->
                val updatedMeasurement = measurement.copy(address = getAddress(measurement))
                if (repository.sendMeasurementToBackend(updatedMeasurement)) {
                    repository.markAsSynced(measurement.id)
                }
            }
        }
    }

    private fun getAddress(measurement: Measurement): String? {
        return GetAddressUseCase(context).execute(measurement.latitude, measurement.longitude)
    }
}
