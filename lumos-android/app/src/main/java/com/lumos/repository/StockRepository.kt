package com.lumos.repository

import android.app.Application
import android.util.Log
import androidx.room.withTransaction
import com.lumos.api.ApiExecutor
import com.lumos.api.StockApi
import com.lumos.api.RequestResult
import com.lumos.api.RequestResult.ServerError
import com.lumos.api.RequestResult.SuccessEmptyBody
import com.lumos.data.database.AppDatabase
import com.lumos.domain.model.Deposit
import com.lumos.domain.model.MaterialStock
import com.lumos.domain.model.OrderMaterial
import com.lumos.domain.model.OrderMaterialItem
import com.lumos.domain.model.Stockist
import com.lumos.midleware.SecureStorage
import com.lumos.utils.Utils.uuidToShortCodeWithPrefix
import com.lumos.worker.SyncManager
import kotlinx.coroutines.flow.Flow
import retrofit2.Retrofit
import java.util.UUID

class StockRepository(
    private val db: AppDatabase,
    api: Retrofit,
    private val secureStorage: SecureStorage,
    private val app: Application
) {
    private val stockApi = api.create(StockApi::class.java)

    suspend fun hasTypesInQueue(types: List<String>): Boolean {
        return db.queueDao().hasTypesInQueue(types)
    }

    suspend fun queueGetStock() {
        SyncManager.queueGetStock(
            context = app.applicationContext,
            db = db,
        )
    }

    fun getMaterialsFlow(): Flow<List<MaterialStock>> {
        return db.stockDao().getMaterialsFlow()
    }

    fun getMaterialsTruckStockControlFlow(): Flow<List<MaterialStock>> {
        return db.stockDao().getMaterialsTruckStockControlFlow()
    }

    fun getMaterialsOrder(): Flow<List<MaterialStock>> {
        return db.stockDao().getMaterialsOrderFlow()
    }

    fun getDepositsFlow(): Flow<List<Deposit>> {
        return db.stockDao().getDepositsFlow()
    }

    fun getStockistsFlow(): Flow<List<Stockist>> {
        return db.stockDao().getStockistsFlow()
    }

    suspend fun callGetStock(): RequestResult<Unit> {
        val teamId = secureStorage.getTeamId()
        val response = ApiExecutor.execute {
            stockApi.getStock(teamId)
        }

        return when (response) {
            is RequestResult.Success -> {
                db.stockDao().insertMaterials(response.data.materialsStock)

                db.stockDao().deleteDeposits()
                db.stockDao().insertDeposits(response.data.deposits)

                db.stockDao().deleteStockists()
                db.stockDao().insertStockists(response.data.stockists)

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

    suspend fun saveOrder(materials: Set<Long>, depositId: Long): String {
        val orderId = UUID.randomUUID().toString()
        val orderCode = uuidToShortCodeWithPrefix("REQ", orderId)

        val order = OrderMaterial(
            orderId = orderId,
            orderCode = orderCode,
            depositId = depositId
        )

        val orderItems = materials.map {
            OrderMaterialItem(
                orderId = orderId,
                materialId = it
            )
        }

        try {
            db.withTransaction {
                db.stockDao().insertOrder(order)
                db.stockDao().insertOrderItems(orderItems)

                SyncManager.queuePostOrder(
                    context = app.applicationContext,
                    db = db,
                    id = orderId
                )
            }
        } catch (e: Exception) {
            throw IllegalStateException("Erro ao salvar pedido: ${e.message}")
        }

        return orderCode
    }


    suspend fun callPostOrder(orderId: String): RequestResult<Unit> {
        val uuid = secureStorage.getUserUuid()
        val order = db.stockDao().getOrderWithItems(orderId)
        val teamId = secureStorage.getTeamId()

        val response = ApiExecutor.execute {
            stockApi.sendOrder(
                uuid = uuid ?: throw IllegalStateException("UUID do usuário atual não encontrado"),
                order = order,
                teamId = if (teamId == 0L) null else teamId
            )
        }

        return when (response) {
            is RequestResult.Success -> {
                RequestResult.Success(Unit)
            }

            is SuccessEmptyBody -> {
                RequestResult.Success(Unit)
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