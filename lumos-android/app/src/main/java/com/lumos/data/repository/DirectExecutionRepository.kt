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
import com.lumos.domain.model.DirectExecutionStreet
import com.lumos.domain.model.DirectExecutionStreetItem
import com.lumos.domain.model.DirectReserve
import com.lumos.domain.model.ExecutionHolder
import com.lumos.domain.model.SendDirectExecutionDto
import com.lumos.midleware.SecureStorage
import com.lumos.utils.Utils.compressImageFromUri
import com.lumos.worker.SyncManager
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.collections.forEach

class DirectExecutionRepository(
    private val db: AppDatabase,
    private val api: ExecutionApi,
    private val secureStorage: SecureStorage,
    private val app: Application
) {
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
                type = "INSTALLATION",
                itemsQuantity = executionDto.reserves.size,
                creationDate = executionDto.creationDate,
                contractor = executionDto.contractor,
                instructions = executionDto.instructions,
            )

            db.directExecutionDao().insertExecution(execution)

            val reservations = executionDto.reserves.map { r ->
                DirectReserve(
                    materialStockId = r.materialStockId,
                    contractItemId = r.contractItemId,
                    contractId = executionDto.contractId,
                    materialName = r.materialName,
                    materialQuantity = r.materialQuantity,
                    requestUnit = r.requestUnit
                )
            }

            db.directExecutionDao().insertReservations(reservations)
        }
    }


    fun getFlowDirectExecutions(): Flow<List<ExecutionHolder>> =
        db.directExecutionDao().getFlowDirectExecutions()

    suspend fun getExecution(contractId: Long): DirectExecution? =
        db.directExecutionDao().getExecution(contractId)

    suspend fun getReservesOnce(contractId: Long): List<DirectReserve> =
        db.directExecutionDao().getReservesOnce(contractId)

    suspend fun debitMaterial(materialStockId: Long, contractId: Long, quantityExecuted: Double) {
        db.directExecutionDao().debitMaterial(materialStockId, contractId, quantityExecuted)
    }

    suspend fun createStreet(street: DirectExecutionStreet): Long =
        db.directExecutionDao().createStreet(street)

    suspend fun createStreetItem(item: DirectExecutionStreetItem) =
        db.directExecutionDao().insertDirectExecutionStreetItem(item)

    suspend fun queuePostDirectExecution(streetId: Long) {
        SyncManager.queuePostDirectExecution(
            context = app.applicationContext,
            db = db,
            streetId = streetId
        )
    }

    suspend fun postDirectExecution(streetId: Long): RequestResult<Unit> {
        val gson = Gson()

        val photoUri = db.directExecutionDao().getPhotoUri(streetId)
        if (photoUri == null) {
            return ServerError(-1, "Foto da pré-medição não encontrada")
        }
        val street = db.directExecutionDao().getStreet(streetId)
        val materials = db.directExecutionDao().getStreetItems(streetId)
        val dto = SendDirectExecutionDto(
            contractId = street.contractId,
            contractor = street.contractor,
            deviceStreetId = street.directStreetId,
            deviceId = street.deviceId,
            latitude = street.latitude,
            longitude = street.longitude,
            address = street.address,
            lastPower = street.lastPower,
            materials = materials,
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

            val response =
                ApiExecutor.execute {
                    api.uploadDirectExecutionData(
                        photo = imagePart,
                        execution = jsonBody
                    )
                }

            return when (response) {
                is RequestResult.Success -> {
                    db.directExecutionDao().deleteStreet(streetId)
                    db.directExecutionDao().deleteItems(streetId)

                    RequestResult.Success(Unit)
                }

                is SuccessEmptyBody -> {
                    db.directExecutionDao().deleteStreet(streetId)
                    db.directExecutionDao().deleteStreet(streetId)

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

    suspend fun finishedDirectExecution(contractId: Long): RequestResult<Unit> {
        val response = ApiExecutor.execute { api.finishDirectExecution(contractId = contractId) }

        return when (response) {
            is RequestResult.Success -> {
                db.directExecutionDao().deleteDirectExecution(contractId)
                db.directExecutionDao().deleteDirectReserves(contractId)

                RequestResult.Success(Unit)
            }

            is SuccessEmptyBody -> {
                db.directExecutionDao().deleteDirectExecution(contractId)
                db.directExecutionDao().deleteDirectReserves(contractId)

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


    suspend fun markAsFinished(contractId: Long) {
        db.directExecutionDao().markAsFinished(contractId)
        SyncManager.markAsDirectExecutionAsFinished(
            context = app.applicationContext,
            db = db,
            contractId = contractId
        )
    }

}