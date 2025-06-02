package com.lumos.data.repository

import android.content.Context
import android.util.Log
import com.lumos.data.api.ExecutionApi
import com.lumos.data.database.AppDatabase
import com.lumos.domain.model.Execution
import com.lumos.domain.model.ExecutionDTO
import com.lumos.domain.model.Reserve
import com.lumos.worker.SyncManager
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import java.util.UUID

class ExecutionRepository(
    private val db: AppDatabase,
    private val api: ExecutionApi
) {

    suspend fun syncExecutions(): Boolean {
        return try {
            val response = api.getExecutions(UUID.fromString(""))
            if (response.isSuccessful) {
                val body = response.body() ?: return false
                saveExecutionsToDb(body)
                true
            } else {
                val code = response.code()
                Log.e("Sync", "Erro de resposta: $code")
                false
                // TODO handle the error
            }
        } catch (e: HttpException) {
            Log.e("Sync", "HttpException: ${e.code()}")
            false
        } catch (e: Exception) {
            Log.e("Sync", "Erro inesperado: ${e.localizedMessage}")
            false
        }
    }

    private suspend fun saveExecutionsToDb(fetchedExecutions: List<ExecutionDTO>) {
        fetchedExecutions.forEach { executionDto ->
            val execution = Execution(
                streetId = executionDto.streetId,

                streetName = executionDto.streetName,
                streetNumber = executionDto.streetNumber,
                streetHood = executionDto.streetHood,
                city = executionDto.city,
                state = executionDto.state,

                teamId = executionDto.teamId,
                teamName = executionDto.teamName,
                executionStatus = "PENDING",
                priority = executionDto.priority,
                type = executionDto.type,
                itemsQuantity = executionDto.itemsQuantity,
                creationDate = executionDto.creationDate,
                latitude = executionDto.latitude,
                longitude = executionDto.longitude,
            )

            db.executionDao().insertExecution(execution)

            executionDto.reserves.forEach { r ->
                val reserve = Reserve(
                    reserveId = r.reserveId,
                    materialId = r.materialId,
                    materialName = r.materialName,
                    materialQuantity = r.materialQuantity,
                    reserveStatus = r.reserveStatus,
                    streetId = executionDto.streetId,
                    depositId = r.depositId,
                    depositName = r.depositName,
                    depositAddress = r.depositAddress,
                    stockistName = r.stockistName,
                    phoneNumber = r.phoneNumber,
                    requestUnit = r.requestUnit
                )
                db.executionDao().insertReserve(reserve)
            }
        }
    }

    fun getFlowExecutions(): Flow<List<Execution>> =
        db.executionDao().getFlowExecutions()

    fun getFlowReserves(streetId: Long, status: List<String>): Flow<List<Reserve>> =
        db.executionDao().getFlowReserves(streetId, status)

    suspend fun queueSyncExecutions(context: Context) {
        SyncManager.queueSyncExecutions(context, db)
    }

    suspend fun setReserveStatus(streetId: Long, status: String = ReservationStatus.COLLECTED) {
        db.executionDao().setReserveStatus(streetId, status)
    }

    suspend fun setExecutionStatus(streetId: Long, status: String = Status.IN_PROGRESS) {
        db.executionDao().setExecutionStatus(streetId, status)
    }

    suspend fun queueSyncFetchReservationStatus(context: Context, streetId: Long, status: String) {
        SyncManager.queueSyncPostGeneric(
            context = context,
            db = db,
            table = "tb_material_reservation",
            field = "status",
            set = status,
            where = "pre_measurement_street_id",
            equal = streetId.toString())
    }

    suspend fun getExecution(lng: Long): Execution {
        return db.executionDao().getExecution(lng)
    }

    suspend fun queueSyncStartExecution(context: Context, streetId: Long) {
        SyncManager.queueSyncPostGeneric(
            context = context,
            db = db,
            table = "tb_pre_measurements_streets",
            field = "street_status",
            set = Status.IN_PROGRESS,
            where = "pre_measurement_street_id",
            equal = streetId.toString())
    }

    suspend fun setPhotoUri(photoUri: String, streetId: Long){
        db.executionDao().setPhotoUri(photoUri, streetId)
    }

    suspend fun finishMaterial(materialId: Long, streetId: Long, quantityExecuted: Double){
        db.executionDao().finishMaterial(
            materialId = materialId,
            streetId = streetId,
            quantityExecuted = quantityExecuted
        )
    }

    suspend fun queuePostExecution(context: Context, streetId: Long) {
        SyncManager.queuePostExecution(
            context = context,
            db = db,
            streetId = streetId
        )
    }

    fun postExecution(): Boolean {
        TODO("Not yet implemented")
    }

}