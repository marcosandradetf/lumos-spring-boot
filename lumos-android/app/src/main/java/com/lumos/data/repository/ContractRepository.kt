package com.lumos.data.repository

import android.content.Context
import android.util.Log
import com.lumos.data.api.ApiExecutor
import com.lumos.data.api.ContractApi
import com.lumos.data.api.RequestResult
import com.lumos.data.database.AppDatabase
import com.lumos.domain.model.Contract
import com.lumos.domain.model.Item
import com.lumos.worker.SyncManager
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException

class ContractRepository(
    private val db: AppDatabase,
    private val api: ContractApi
) {

    suspend fun syncContracts(context: Context): RequestResult<Unit>  {
        val response = ApiExecutor.execute { api.getContracts() }

        return when (response) {
            is RequestResult.Success -> {
                db.contractDao().insertContracts(response.data)
                RequestResult.Success(Unit)
            }

            is RequestResult.NoInternet -> {
                SyncManager.queueSyncContracts(context, db)
                RequestResult.NoInternet
            }

            is RequestResult.Timeout -> RequestResult.Timeout
            is RequestResult.ServerError -> RequestResult.ServerError(response.code, response.message)
            is RequestResult.UnknownError -> {
                Log.e("Sync", "Erro desconhecido", response.error)
                RequestResult.UnknownError(response.error)
            }
        }

    }

    suspend fun syncContractItems(context: Context): RequestResult<Unit> {
        val response = ApiExecutor.execute { api.getItems() }

        return when (response) {
            is RequestResult.Success -> {
                db.contractDao().insertItems(response.data) // lista direta
                RequestResult.Success(Unit)
            }
            is RequestResult.NoInternet -> {
                SyncManager.queueSyncContractItems(context, db)
                RequestResult.NoInternet
            }
            is RequestResult.Timeout -> RequestResult.Timeout
            is RequestResult.ServerError -> RequestResult.ServerError(response.code, response.message)
            is RequestResult.UnknownError -> {
                Log.e("Sync", "Erro desconhecido", response.error)
                RequestResult.UnknownError(response.error)
            }
        }
    }

    fun getFlowContracts(status: String): Flow<List<Contract>> =
        db.contractDao().getFlowContracts(status)


    suspend fun getContract(contractId: Long): Contract? {
        return db.contractDao().getContract(contractId)
    }

    suspend fun setStatus(contractId: Long, status: String) {
        db.contractDao().setStatus(contractId, status)
    }

    suspend fun startAt(contractId: Long, updated: String, deviceId: String) {
        db.contractDao().startAt(contractId, updated, deviceId)
    }

    suspend fun queueSyncContracts(context: Context) {
        SyncManager.queueSyncContracts(
            context,
            db
        )
    }


    fun getItemsFromContract(powers: List<String>): Flow<List<Item>> =
        db.contractDao().getItemsFromContract(powers)


}