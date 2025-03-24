package com.lumos.domain.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lumos.data.api.ApiService
import com.lumos.data.api.MeasurementApi
import com.lumos.data.database.AppDatabase
import com.lumos.data.repository.MeasurementRepository
import com.lumos.domain.model.PreMeasurementStreet
import com.lumos.midleware.SecureStorage
import com.lumos.utils.ConnectivityUtils

class SyncMeasurement(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    private val repository: MeasurementRepository
    private var address: String? = null
    private val secureStorage: SecureStorage = SecureStorage(appContext)


    init {
        val api = ApiService(secureStorage)
        val measurementApi = api.createApi(MeasurementApi::class.java)

        repository = MeasurementRepository(
            AppDatabase.getInstance(appContext).measurementDao(),
            measurementApi,
            appContext
        )
    }

    override suspend fun doWork(): Result {
        return try {
            if (ConnectivityUtils.isNetworkGood(applicationContext)) {
                val unsyncedData = repository.getUnSyncedMeasurements()
                unsyncedData.forEach { measurement ->
                    var updatedMeasurement = measurement
                    if (measurement.address == null) {
                        setAddress(measurement)
                        updatedMeasurement = measurement.copy(address = address)
                    }
                    val items = repository.getItems(measurement.preMeasurementStreetId)
                    if (repository.sendMeasurementToBackend(updatedMeasurement, items, secureStorage.getUserUuid()!!)) {
                        repository.markAsSynced(measurement.preMeasurementStreetId)
                        Result.success()
                    } else {
                        Result.retry()
                    }
                }
                Result.success()
            } else Result.retry()
        } catch (e: Exception) {
            Result.retry() // Retenta em caso de falha
        }
    }

    private fun setAddress(preMeasurementStreet: PreMeasurementStreet) {
        val address =
            AddressService(applicationContext).execute(preMeasurementStreet.latitude, preMeasurementStreet.longitude)
        this.address = address?.get(0)
    }

}
