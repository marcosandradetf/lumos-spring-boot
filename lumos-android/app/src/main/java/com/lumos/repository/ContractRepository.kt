package com.lumos.repository

import android.app.Application
import android.util.Log
import com.lumos.api.ApiExecutor
import com.lumos.api.ContractApi
import com.lumos.api.RequestResult
import com.lumos.api.RequestResult.ServerError
import com.lumos.api.RequestResult.SuccessEmptyBody
import com.lumos.data.database.AppDatabase
import com.lumos.domain.model.Contract
import com.lumos.domain.model.Item
import com.lumos.worker.SyncManager
import com.lumos.worker.SyncTypes
import kotlinx.coroutines.flow.Flow

class ContractRepository(
    private val db: AppDatabase,
    private val api: ContractApi,
    private val app: Application
) {

    suspend fun syncContracts(): RequestResult<Unit>  {
        val response = ApiExecutor.execute { api.getContracts() }

        return when (response) {
            is RequestResult.Success -> {
                // get contracts in use
                db.contractDao().deleteContracts()
                db.contractDao().insertContracts(response.data)
                RequestResult.Success(Unit)
            }

            is SuccessEmptyBody -> {
                ServerError(204, "Resposta 204 inesperada")
            }

            is RequestResult.NoInternet -> {
                SyncManager.queueSyncContracts(app.applicationContext, db)
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

    suspend fun syncContractItems(): RequestResult<Unit> {
        val response = ApiExecutor.execute { api.getItems() }

        return when (response) {
            is RequestResult.Success -> {
                db.contractDao().insertItems(response.data) // lista direta
                RequestResult.Success(Unit)
            }
            is SuccessEmptyBody -> {
                ServerError(204, "Resposta 204 inesperada")
            }
            is RequestResult.NoInternet -> {
                SyncManager.queueSyncContractItems(app.applicationContext, db)
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

    fun getFlowContractsForPreMeasurement(status: String): Flow<List<Contract>> =
        db.contractDao().getFlowContractsForPreMeasurement(status)


    suspend fun getContract(contractId: Long): Contract? {
        return db.contractDao().getContract(contractId)
    }

    suspend fun setStatus(contractId: Long, status: String) {
        db.contractDao().setStatus(contractId, status)
    }

    suspend fun startAt(contractId: Long, updated: String, deviceId: String) {
        db.contractDao().startAt(contractId, updated, deviceId)
    }

    fun getItemsFromContract(ids: List<Long>): Flow<List<Item>> =
        db.contractDao().getItemsFromContract(ids)

    fun getFlowContractsByExecution(longs: List<Long>): Flow<List<Contract>> =
        db.contractDao().getFlowContractsByExecution(longs)

    fun getFlowContractsForMaintenance(): Flow<List<Contract>> =
        db.contractDao().getFlowContractsForMaintenance()

    suspend fun getContractItemBalance(contractId: Long): RequestResult<Unit>  {
        val response = ApiExecutor.execute { api.getContractItemBalance(contractId) }
        println("getContractItemBalance")

        return when (response) {
            is RequestResult.Success -> {
                println(response)
                db.contractDao().insertContractItemBalance(response.data) // lista direta
                println("getContractItemBalance - success")
                RequestResult.Success(Unit)
            }
            is SuccessEmptyBody -> {
                ServerError(204, "Resposta 204 inesperada")
            }
            is RequestResult.NoInternet -> {
                SyncManager.queueSyncContractItems(app.applicationContext, db)
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

    suspend fun checkBalance(): Boolean {
        println("checkBalance")
        val count = db.queueDao().countPendingItemsByTypes(listOf(SyncTypes.POST_DIRECT_EXECUTION, SyncTypes.SUBMIT_PRE_MEASUREMENT_INSTALLATION_STREET))
        return count == 0
    }


}