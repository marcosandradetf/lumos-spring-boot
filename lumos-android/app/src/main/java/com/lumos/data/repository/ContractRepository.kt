package com.lumos.data.repository

import android.content.Context
import android.util.Log
import com.lumos.data.api.ContractApi
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

    suspend fun syncContracts(): Boolean {
        var remoteContracts: List<Contract> = emptyList()

        try {
            val response = api.getContracts()
            if (response.isSuccessful) {
                val body = response.body()
                remoteContracts = body!!

            } else {
                val code = response.code()
                // TODO handle the error
            }
        } catch (e: HttpException) {
            val response = e.response()
            val errorCode = e.code()
            // TODO handle the error
        }

        if (remoteContracts.isNotEmpty()) {
            remoteContracts.forEach { db.contractDao().insertContract(it) }
            return true
        }
        return false
    }

    fun getFlowContracts(status: String): Flow<List<Contract>> =
        db.contractDao().getFlowContracts(status)


    suspend fun getContract(contractId: Long): Contract {
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

    suspend fun syncContractItems(): Boolean {
        var remoteItems: List<Item> = emptyList()
        Log.e("tag", "buscando itens")
        try {
            val response = api.getItems()
            if (response.isSuccessful) {
                val body = response.body()
                remoteItems = body!!
                Log.e("response", remoteItems.toString())

            } else {
                val code = response.code()
                Log.e("code", code.toString())
            }
        } catch (e: HttpException) {
            val response = e.response()
            val errorCode = e.code()
        }

        if (remoteItems.isNotEmpty()) {
            db.contractDao().deleteAllItems()
            remoteItems.forEach { item ->
                db.contractDao().insertItem(item)
            }
            return true
        }
        return false
    }

    fun getItemsFromContract(powers: List<String>): Flow<List<Item>> =
        db.contractDao().getItemsFromContract(powers)


    suspend fun queueSyncContractItems(context: Context) {
        SyncManager.queueSyncContractItems(
            context,
            db
        )
    }
}