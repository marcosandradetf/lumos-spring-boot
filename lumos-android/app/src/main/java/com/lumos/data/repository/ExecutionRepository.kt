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
import com.lumos.domain.model.DirectExecution
import com.lumos.domain.model.DirectExecutionDTOResponse
import com.lumos.domain.model.Execution
import com.lumos.domain.model.ExecutionDTO
import com.lumos.domain.model.ExecutionHolder
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
            val execution = Execution(
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

            db.executionDao().insertExecution(execution)
            db.executionDao().setExecutionStatus(execution.streetId, execution.executionStatus)

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
                    requestUnit = r.requestUnit,
                    contractId = executionDto.contractId
                )
                db.executionDao().insertReserve(reserve)
            }
        }
    }

    suspend fun syncDirectExecutions(): RequestResult<Unit> {
        val uuid = secureStorage.getUserUuid()
            ?: return ServerError(-1, "UUID Não encontrado")

        val response = ApiExecutor.execute { api.getDirectExecutions(uuid) }
        return when (response) {
            is RequestResult.Success -> {
                saveDirectExecutionsToDb(response.data)
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

    private suspend fun saveDirectExecutionsToDb(fetchedExecutions: List<DirectExecutionDTOResponse>) {
        fetchedExecutions.forEach { executionDto ->
            val execution = DirectExecution(
                contractId = executionDto.contractId,
                executionStatus = "PENDING",
                type = "",
                itemsQuantity = executionDto.reserves.size,
                creationDate = "",
                contractor = executionDto.contractor,
                instructions = executionDto.instructions
            )



            db.executionDao().insertDirectExecution(execution)

            executionDto.reserves.forEach { r ->
                val reserve = Reserve(
                    reserveId = r.reserveId,
                    materialName = r.materialName,
                    materialQuantity = r.materialQuantity,
                    reserveStatus = r.reserveStatus,
                    streetId = -1,
                    contractId = executionDto.contractId,
                    depositId = r.depositId,
                    depositName = r.depositName,
                    depositAddress = r.depositAddress,
                    stockistName = r.stockistName,
                    phoneNumber = r.phoneNumber,
                    requestUnit = r.requestUnit,
                )
                db.executionDao().insertReserve(reserve)
            }
        }
    }

    fun getFlowExecutions(): Flow<List<ExecutionHolder>> =
        db.executionDao().getFlowExecutions()

    fun getFlowReserves(streetId: Long, status: List<String>): Flow<List<Reserve>> =
        db.executionDao().getFlowReserves(streetId, status)

    suspend fun setReserveStatus(streetId: Long, status: String = ReservationStatus.COLLECTED) {
        db.executionDao().setReserveStatus(streetId, status)
    }

    suspend fun setExecutionStatus(streetId: Long, status: String = ExecutionStatus.IN_PROGRESS) {
        db.executionDao().setExecutionStatus(streetId, status)
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

    suspend fun getExecution(lng: Long): Execution? {
        return db.executionDao().getExecution(lng)
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
        db.executionDao().setPhotoUri(photoUri, streetId)
    }

    suspend fun finishMaterial(reserveId: Long, quantityExecuted: Double) {
        db.executionDao().finishMaterial(
            reserveId = reserveId,
            quantityExecuted = quantityExecuted
        )
    }

    suspend fun queuePostExecution(streetId: Long) {
        SyncManager.queuePostExecution(
            context = app.applicationContext,
            db = db,
            streetId = streetId
        )
    }


    suspend fun postExecution(streetId: Long ): RequestResult<Unit> {
        val gson = Gson()

        val photoUri = db.executionDao().getPhotoUri(streetId)
        if(photoUri == null) {
            return ServerError(-1, "Foto da pré-medição não encontrada")
        }
        val reserves = db.executionDao().getReservesPartial(streetId)
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
                    db.executionDao().deleteExecution(streetId)
                    db.executionDao().deleteReserves(streetId)

                    RequestResult.Success(Unit)
                }
                is SuccessEmptyBody -> {
                    db.executionDao().deleteExecution(streetId)
                    db.executionDao().deleteReserves(streetId)

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

    suspend fun getReservesOnce(streetId: Long, statusList: List<String>): List<Reserve> {
        return db.executionDao().getReservesOnce(streetId, statusList)
    }

    suspend fun getExecutionsByContract(lng: Long): List<Execution> {
        return db.executionDao().getExecutionsByContract(lng)
    }


    fun getFlowDirectExecutions(): Flow<List<ExecutionHolder>> =
        db.executionDao().getFlowDirectExecutions()
}