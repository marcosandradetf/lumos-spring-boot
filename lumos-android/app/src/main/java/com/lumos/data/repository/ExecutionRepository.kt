package com.lumos.data.repository

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import com.google.gson.Gson
import com.lumos.data.api.ApiExecutor
import com.lumos.data.api.ExecutionApi
import com.lumos.data.api.RequestResult
import com.lumos.data.database.AppDatabase
import com.lumos.domain.model.Execution
import com.lumos.domain.model.ExecutionDTO
import com.lumos.domain.model.Reserve
import com.lumos.domain.model.SendExecutionDto
import com.lumos.midleware.SecureStorage
import com.lumos.utils.Utils.compressImageFromUri
import com.lumos.worker.SyncManager
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class ExecutionRepository(
    private val db: AppDatabase,
    private val api: ExecutionApi,
    private val secureStorage: SecureStorage
) {

    suspend fun syncExecutions(context: Context): RequestResult<Unit> {
        val uuid = secureStorage.getUserUuid()
            ?: return RequestResult.ServerError(-1, "UUID Não encontrado")

        val response = ApiExecutor.execute { api.getExecutions(uuid) }
        return when (response) {
            is RequestResult.Success -> {
                saveExecutionsToDb(response.data)
                RequestResult.Success(Unit)
            }

            is RequestResult.NoInternet -> {
                SyncManager.queueSyncExecutions(context, db)
                RequestResult.NoInternet
            }

            is RequestResult.Timeout -> RequestResult.Timeout
            is RequestResult.ServerError -> RequestResult.ServerError(
                response.code,
                response.message
            )

            is RequestResult.UnknownError -> {
                Log.e("Sync", "Erro desconhecido", response.error)
                RequestResult.UnknownError(response.error)
            }
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
            equal = streetId.toString()
        )
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
            equal = streetId.toString()
        )
    }

    suspend fun setPhotoUri(photoUri: String, streetId: Long) {
        db.executionDao().setPhotoUri(photoUri, streetId)
    }

    suspend fun finishMaterial(reserveId: Long, quantityExecuted: Double) {
        db.executionDao().finishMaterial(
            reserveId = reserveId,
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


    suspend fun postExecution(streetId: Long, context: Context): Boolean {
        val gson = Gson()

        val photoUri = db.executionDao().getPhotoUri(streetId)
        val reserves = db.executionDao().getReservesPartial(streetId)
        val dto = SendExecutionDto(
            streetId = streetId,
            reserves = reserves
        )

        val json = gson.toJson(dto)
        val jsonBody = json.toRequestBody("application/json".toMediaType())

        val byteArray = compressImageFromUri(context, photoUri.toUri())
        byteArray?.let {
            val requestFile = it.toRequestBody("image/jpeg".toMediaType())
            val imagePart = MultipartBody.Part.createFormData(
                "photo",
                "upload_${System.currentTimeMillis()}.jpg",
                requestFile
            )
            val response = api.uploadData(photo = imagePart, execution = jsonBody)

            return try {
                response.isSuccessful
            } catch (e: Exception) {
                false
            }
        }

        return false
    }

}