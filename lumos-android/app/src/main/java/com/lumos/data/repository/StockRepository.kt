package com.lumos.data.repository

import android.content.Context
import com.lumos.data.api.StockApi
import com.lumos.data.database.AppDatabase
import com.lumos.domain.model.Material
import com.lumos.worker.SyncManager
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException

class StockRepository(
    private val db: AppDatabase,
    private val api: StockApi,
) {

    private suspend fun getCountDeposits(): Int {
        return db.stockDao().getCountDeposits()
    }



    suspend fun syncMaterials(): Boolean {
        var remoteMaterials: List<Material> = emptyList()

        try {
            val response = api.getMaterials()
            if (response.isSuccessful) {
                val body = response.body()
                remoteMaterials = body!!

            } else {
                val code = response.code()
            }
        } catch (e: HttpException) {
            val response = e.response()
            val errorCode = e.code()
        }

        if(remoteMaterials.isNotEmpty()) {
            db.stockDao().deleteAll()
            remoteMaterials.forEach { material ->
                db.stockDao().insertMaterial(material)
            }
            return true
        }
        return false
    }

    fun getMaterialsOfContract(powers: List<String>, lengths: List<String>): Flow<List<Material>> =
        db.stockDao().getMaterialsOfContract(powers, lengths)


    suspend fun queueSyncStock(context: Context) {
        SyncManager.queueSyncStock(
            context,
            db
        )
    }


}