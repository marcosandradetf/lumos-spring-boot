package com.lumos.data.repository

import android.app.Application
import androidx.room.withTransaction
import com.lumos.data.api.ApiExecutor
import com.lumos.data.api.ApiService
import com.lumos.data.api.MaintenanceApi
import com.lumos.data.api.RequestResult
import com.lumos.data.database.AppDatabase
import com.lumos.domain.model.Maintenance
import com.lumos.domain.model.MaintenanceStreet
import com.lumos.domain.model.MaintenanceStreetItem
import com.lumos.worker.SyncManager
import kotlinx.coroutines.flow.Flow

class MaintenanceRepository(
    private val db: AppDatabase,
    api: ApiService,
    private val app: Application
) {
    private val maintenanceApi = api.createApi(MaintenanceApi::class.java)

    suspend fun insertMaintenance(maintenance: Maintenance) {
        db.maintenanceDao().insertMaintenance(maintenance)
    }

    suspend fun insertMaintenanceStreet(maintenanceStreet: MaintenanceStreet, items: List<MaintenanceStreetItem>) {
        db.withTransaction {
            db.maintenanceDao().insertMaintenanceStreet(maintenanceStreet)
            db.maintenanceDao().insertMaintenanceStreetItems(items)
            for (item in items) {
                db.stockDao().debitStock(item.materialStockId, item.quantityExecuted)
            }
            SyncManager.queuePostMaintenanceStreet(
                context = app.applicationContext,
                db = db,
                id = maintenanceStreet.maintenanceStreetId
            )
        }
    }

    suspend fun getItemsByStreetId(maintenanceStreetId: String): List<MaintenanceStreetItem> {
        return db.maintenanceDao().getItemsByStreetId(maintenanceStreetId)
    }

    fun getFlowMaintenance(status: String): Flow<List<Maintenance>> {
        return db.maintenanceDao().getFlowMaintenance(status)
    }

    fun getFlowStreets(maintenanceId: String): Flow<List<MaintenanceStreet>> {
        return db.maintenanceDao().getFlowStreets(maintenanceId)
    }

    suspend fun queuePostMaintenance(maintenanceId: String) {
        SyncManager.queuePostMaintenance(
            context = app.applicationContext,
            db = db,
            maintenanceId = maintenanceId
        )
    }

    suspend fun callPostMaintenance(maintenanceId: String): RequestResult<Unit> {
        val maintenance = db.maintenanceDao().getMaintenance(maintenanceId)

        val response = ApiExecutor.execute { maintenanceApi.finishMaintenance(maintenance) }

        return when (response) {
            is RequestResult.Success -> {
                db.maintenanceDao().deleteMaintenance(maintenanceId)
                RequestResult.Success(Unit)
            }

            is RequestResult.SuccessEmptyBody -> {
                db.maintenanceDao().deleteMaintenance(maintenanceId)
                RequestResult.Success(Unit)
            }

            is RequestResult.Timeout -> {
                RequestResult.Timeout
            }

            is RequestResult.NoInternet -> {
                RequestResult.NoInternet
            }

            is RequestResult.ServerError -> {
                RequestResult.ServerError(response.code, response.message)
            }

            is RequestResult.UnknownError -> {
                RequestResult.UnknownError(response.error)
            }
        }
    }

    suspend fun callPostMaintenanceStreet(maintenanceStreetId: String): RequestResult<Unit> {
        val street = db.maintenanceDao().getMaintenanceStreetWithItems(maintenanceStreetId)

        val response = ApiExecutor.execute { maintenanceApi.sendStreet(street) }

        return when (response) {
            is RequestResult.Success -> {
                RequestResult.Success(Unit)
            }

            is RequestResult.SuccessEmptyBody -> {
                RequestResult.Success(Unit)
            }

            is RequestResult.Timeout -> {
                RequestResult.Timeout
            }

            is RequestResult.NoInternet -> {
                RequestResult.NoInternet
            }

            is RequestResult.ServerError -> {
                RequestResult.ServerError(response.code, response.message)
            }

            is RequestResult.UnknownError -> {
                RequestResult.UnknownError(response.error)
            }
        }
    }

    suspend fun getMaintenanceIdByContractId(contractId: Long): String? {
        return db.maintenanceDao().getMaintenanceIdByContractId(contractId)
    }

}