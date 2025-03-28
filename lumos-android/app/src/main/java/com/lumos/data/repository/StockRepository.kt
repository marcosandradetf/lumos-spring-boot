package com.lumos.data.repository

import android.util.Log
import com.lumos.data.database.StockDao
import com.lumos.domain.model.Deposit
import com.lumos.data.api.StockApi
import com.lumos.domain.model.Material
import retrofit2.HttpException

class StockRepository(
    private val dao: StockDao,
    private val api: StockApi
) {

    private suspend fun getCountDeposits(): Int {
        return dao.getCountDeposits()

    }

    suspend fun syncDeposits() {
        val countDepositsOffline: Int = getCountDeposits()

        var depositsOnline: List<Deposit> = emptyList()

        try {
            val response = api.getDeposits()
            if (response.isSuccessful) {
                val body = response.body()
                depositsOnline = body!!
            } else {
                val code = response.code()
                // TODO handle the error
            }
        } catch (e: HttpException) {
            val response = e.response()
            val errorCode = e.code()
            // TODO handle the error
        }

        val countDepositsOnline: Int = depositsOnline.count()

        if (countDepositsOnline != countDepositsOffline) {
            depositsOnline.forEach { deposit ->
                dao.insertDeposit(deposit)
            }
        }

    }

    suspend fun getDepositByRegion(regionName: String): Deposit? {
        return dao.getDepositByRegion(regionName)

    }

    suspend fun getAllDeposits(): List<Deposit> {
        return dao.getDeposits()
    }

    suspend fun syncMaterials() {
        var remoteMaterials: List<Material> = emptyList()

        try {
            val response = api.getMaterials()
            if (response.isSuccessful) {
                val body = response.body()
                remoteMaterials = body!!

            } else {
                val code = response.code()
                // TODO handle the error
            }
        } catch (e: HttpException) {
            val response = e.response()
            val errorCode = e.code()
            // TODO handle the error
        }

            dao.deleteAll()
            remoteMaterials.forEach { material ->
                dao.insertMaterial(material)
            }
    }

    suspend fun getMaterials(): List<Material> {
        return dao.getMaterials()
    }

    suspend fun firstSync() {
        if (dao.getCountDeposits() == 0 || dao.getCountMaterials() == 0) {
            syncMaterials()
            syncDeposits()
        }
    }

}