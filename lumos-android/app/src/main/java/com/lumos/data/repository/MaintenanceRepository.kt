package com.lumos.data.repository

import android.app.Application
import android.util.Log
import com.lumos.data.api.ApiExecutor
import com.lumos.data.api.ApiService
import com.lumos.data.api.MaintenanceApi
import com.lumos.data.api.RequestResult
import com.lumos.data.api.RequestResult.ServerError
import com.lumos.data.api.RequestResult.SuccessEmptyBody
import com.lumos.data.database.AppDatabase
import com.lumos.domain.model.MaterialStock
import com.lumos.midleware.SecureStorage
import com.lumos.worker.SyncManager
import kotlinx.coroutines.flow.Flow

class MaintenanceRepository(
    private val db: AppDatabase,
    private val api: ApiService,
    private val secureStorage: SecureStorage,
    private val app: Application
) {
    private val maintenanceApi = api.createApi(MaintenanceApi::class.java)

    fun getFlowExistsTypeInQueue(types: List<String>): Flow<Boolean> {
        return db.queueDao().getFlowExistsTypeInQueue(types)
    }

    suspend fun queueGetStock() {
        SyncManager.queueGetStock(
            context = app.applicationContext,
            db = db,
        )
    }

    suspend fun queuePostMaintenance(id: Long) {
        SyncManager.queuePostMaintenance(
            context = app.applicationContext,
            db = db,
            id = id
        )
    }

    fun getMaterialsFlow(): Flow<List<MaterialStock>> {
        return db.maintenanceDao().getMaterialsFlow()
    }

    suspend fun callGetStock(): RequestResult<Unit> {
        val uuid = secureStorage.getUserUuid()

        val response = ApiExecutor.execute { maintenanceApi.getStock(uuid ?: throw IllegalStateException("UUID do usuário atual não encontrado")) }

        return when (response) {
            is RequestResult.Success -> {
                db.maintenanceDao().insertMaterials(response.data)
                RequestResult.Success(Unit)
            }
            is SuccessEmptyBody -> {
                ServerError(204, "Resposta 204 inesperada")
            }
            is RequestResult.NoInternet -> {
                RequestResult.NoInternet
            }
            is RequestResult.Timeout -> RequestResult.Timeout
            is ServerError -> ServerError(response.code, response.message)
            is RequestResult.UnknownError -> {
                Log.e("Sync", "Erro desconhecido", response.error)
                RequestResult.UnknownError(response.error)
            }
        }

    }

}