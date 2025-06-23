package com.lumos.data.repository

import android.app.Application
import android.util.Log
import androidx.core.net.toUri
import com.google.gson.Gson
import com.lumos.data.api.ApiExecutor
import com.lumos.data.api.ExecutionApi
import com.lumos.data.api.RequestResult
import com.lumos.data.api.RequestResult.ServerError
import com.lumos.data.api.RequestResult.SuccessEmptyBody
import com.lumos.data.database.AppDatabase
import com.lumos.domain.model.ExecutionDTO
import com.lumos.domain.model.ExecutionHolder
import com.lumos.domain.model.IndirectExecution
import com.lumos.domain.model.IndirectReserve
import com.lumos.domain.model.SendExecutionDto
import com.lumos.midleware.SecureStorage
import com.lumos.utils.Utils.compressImageFromUri
import com.lumos.worker.SyncManager
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class IndirectExecutionRepository(
    private val db: AppDatabase,
    private val api: ExecutionApi,
    private val secureStorage: SecureStorage,
    private val app: Application
) {

    suspend fun syncExecutions(): RequestResult<Unit> {
        val uuid = secureStorage.getUserUuid()
            ?: return ServerError(-1, "UUID Não encontrado")

        val response = ApiExecutor.execute { api.getExecutions(uuid) }
        return when (response) {
            is RequestResult.Success -> {
                saveExecutionsToDb(response.data)
                RequestResult.Success(Unit)
            }
            is SuccessEmptyBody -> {
                ServerError(204, "Resposta 204 inesperada")
            }

            is RequestResult.NoInternet -> {
                SyncManager.queueSyncExecutions(app.applicationContext, db)
                RequestResult.NoInternet
            }

            is RequestResult.Timeout -> RequestResult.Timeout
            is ServerError -> ServerError(
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
            val execution = IndirectExecution(
                streetId = executionDto.streetId,
                streetName = executionDto.streetName,
                streetNumber = executionDto.streetNumber,
                streetHood = executionDto.streetHood,
                city = executionDto.city,
                state = executionDto.state,
                executionStatus = "PENDING",
                priority = executionDto.priority,
                type = executionDto.type,
                itemsQuantity = executionDto.itemsQuantity,
                creationDate = executionDto.creationDate,
                latitude = executionDto.latitude,
                longitude = executionDto.longitude,
                contractId = executionDto.contractId,
                contractor = executionDto.contractor
            )

            db.indirectExecutionDao().insertIndirectExecution(execution)
            db.indirectExecutionDao().setIndirectExecutionStatus(execution.streetId, execution.executionStatus)

            executionDto.reserves.forEach { r ->
                val reserve = IndirectReserve(
                    reserveId = r.reserveId,
                    materialName = r.materialName,
                    materialQuantity = r.materialQuantity,
                    streetId = executionDto.streetId,
                    requestUnit = r.requestUnit,
                    contractId = r.contractId,
                    contractItemId = r.contractItemId,
                )
                db.indirectExecutionDao().insertIndirectReserve(reserve)
            }
        }
    }

    fun getFlowExecutions(): Flow<List<ExecutionHolder>> =
        db.indirectExecutionDao().getFlowIndirectExecution()

    fun getFlowReserves(streetId: Long): Flow<List<IndirectReserve>> =
        db.indirectExecutionDao().getFlowIndirectReserve(streetId)

    suspend fun setExecutionStatus(streetId: Long, status: String = ExecutionStatus.IN_PROGRESS) {
        db.indirectExecutionDao().setIndirectExecutionStatus(streetId, status)
    }

    suspend fun queueSyncFetchReservationStatus(streetId: Long, status: String) {
        SyncManager.queueSyncPostGeneric(
            context = app.applicationContext,
            db = db,
            table = "tb_material_reservation",
            field = "status",
            set = status,
            where = "pre_measurement_street_id",
            equal = streetId.toString(),
        )
    }

    suspend fun getExecution(lng: Long): IndirectExecution? {
        return db.indirectExecutionDao().getExecution(lng)
    }

    suspend fun queueSyncStartExecution(streetId: Long) {
        SyncManager.queueSyncPostGeneric(
            context = app.applicationContext,
            db = db,
            table = "tb_pre_measurements_streets",
            field = "street_status",
            set = ExecutionStatus.IN_PROGRESS,
            where = "pre_measurement_street_id",
            equal = streetId.toString()
        )
    }

    suspend fun setPhotoUri(photoUri: String, streetId: Long) {
        db.indirectExecutionDao().setIndirectExecutionPhotoUri(photoUri, streetId)
    }

    suspend fun finishMaterial(reserveId: Long, quantityExecuted: Double) {
        db.indirectExecutionDao().finishMaterial(
            reserveId = reserveId,
            quantityExecuted = quantityExecuted
        )
    }

    suspend fun queuePostExecution(streetId: Long) {
        SyncManager.queuePostIndirectExecution(
            context = app.applicationContext,
            db = db,
            streetId = streetId
        )
    }

    suspend fun postExecution(streetId: Long ): RequestResult<Unit> {
        val gson = Gson()

        val photoUri = db.indirectExecutionDao().getPhotoUri(streetId)
        if(photoUri == null) {
            return ServerError(-1, "Foto da pré-medição não encontrada")
        }
        val reserves = db.indirectExecutionDao().getReservesPartial(streetId)
        val dto = SendExecutionDto(
            streetId = streetId,
            reserves = reserves
        )

        val json = gson.toJson(dto)
        val jsonBody = json.toRequestBody("application/json".toMediaType())

        val byteArray = compressImageFromUri(app.applicationContext, photoUri.toUri())
        byteArray?.let {
            val requestFile = it.toRequestBody("image/jpeg".toMediaType())
            val imagePart = MultipartBody.Part.createFormData(
                "photo",
                "upload_${System.currentTimeMillis()}.jpg",
                requestFile
            )

            val response = ApiExecutor.execute { api.uploadData(photo = imagePart, execution = jsonBody) }

            return when (response) {
                is RequestResult.Success -> {
                    db.indirectExecutionDao().deleteExecution(streetId)
                    db.indirectExecutionDao().deleteReserves(streetId)

                    RequestResult.Success(Unit)
                }
                is SuccessEmptyBody -> {
                    db.indirectExecutionDao().deleteExecution(streetId)
                    db.indirectExecutionDao().deleteReserves(streetId)

                    SuccessEmptyBody
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

        return ServerError(-1, "Erro na criacao da foto da execucao")
    }

    suspend fun getReservesOnce(streetId: Long): List<IndirectReserve> {
        return db.indirectExecutionDao().getIndirectReserveOnce(streetId)
    }

    suspend fun getExecutionsByContract(lng: Long): List<IndirectExecution> {
        return db.indirectExecutionDao().getExecutionsByContract(lng)
    }

}