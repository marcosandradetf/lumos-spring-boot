package com.lumos.domain.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lumos.data.api.ApiService
import com.lumos.data.api.PreMeasurementApi
import com.lumos.data.api.UserExperience
import com.lumos.data.database.AppDatabase
import com.lumos.data.repository.PreMeasurementRepository
import com.lumos.midleware.SecureStorage
import com.lumos.utils.ConnectivityUtils

class SyncPreMeasurement(
    appContext: Context,
    workerParams: WorkerParameters,
) :
    CoroutineWorker(appContext, workerParams) {
    private val repository: PreMeasurementRepository
    private val secureStorage: SecureStorage = SecureStorage(appContext)

    init {
        val api = ApiService(secureStorage)
        val preMeasurementApi = api.createApi(PreMeasurementApi::class.java)

        repository = PreMeasurementRepository(
            AppDatabase.getInstance(appContext).preMeasurementDao(),
            AppDatabase.getInstance(appContext).contractDao(),
            preMeasurementApi,
            appContext
        )
    }

    override suspend fun doWork(): Result {
        val contractId = inputData.getLong("contract_id", -1L) // Obtendo o parâmetro
        val uuid = secureStorage.getUserUuid()
        if (contractId == -1L || uuid == null) {
            Log.e("SyncPreMeasurement","UUID ou contractId invalid")
            return Result.failure()
        }
        return try {
            if (ConnectivityUtils.isNetworkGood(applicationContext)) {
                val contract = repository.getPreMeasurement(contractId)
                val streets = repository.getStreets(contractId)
                val items = repository.getItems(contractId)
                if (repository.sendMeasurementToBackend(
                        contract,
                        streets,
                        items,
                        secureStorage.getUserUuid()!!
                    )
                ) {
                    repository.finishPreMeasurement(
                        contractId = contract.contractId
                    )
                    Result.success()
                } else {
                    Result.retry()
                }
            } else Result.retry()
        } catch (e: Exception) {
            UserExperience.sendNotification(
                context = applicationContext,
                title = "Erro ao enviar pré-mediçao",
                body = "Tente novamente e caso se repita, contate o Suporte e informe o seguinte erro: 'Código 1 - Exception na classe SyncPreMeasurement'",
            )
            return Result.failure()
        }
    }

}
