package com.lumos.domain.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lumos.data.api.ApiService
import com.lumos.data.api.AuthApi
import com.lumos.data.api.RetrofitClient
import com.lumos.data.api.StockApi
import com.lumos.data.database.AppDatabase
import com.lumos.data.repository.StockRepository
import com.lumos.midleware.SecureStorage
import com.lumos.utils.ConnectivityUtils

class SyncStock(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    private val repository: StockRepository

    init {
        // Reconstruindo a MeasurementApi com os dados passados
        val authApi = RetrofitClient.createService(AuthApi::class.java)
        val secureStorage = SecureStorage(appContext)
        val api = ApiService(secureStorage, authApi)
        val stockApi = api.createApi(StockApi::class.java)

        repository = StockRepository(
            AppDatabase.getInstance(appContext).stockDao(),
            stockApi,
        )

        Log.e("Worker", "Worker Ativado")
    }

    override suspend fun doWork(): Result {
        return try {
            if (ConnectivityUtils.isNetworkGood(applicationContext)) {
                repository.syncDeposits()
                repository.syncMaterials()
            } else Result.retry()
            Result.success()
        } catch (e: Exception) {
            Result.retry() // Retenta em caso de falha
        }
    }

}
