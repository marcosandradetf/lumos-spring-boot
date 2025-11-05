package com.lumos.repository

import android.app.Application
import android.util.Log
import androidx.core.net.toUri
import androidx.room.withTransaction
import com.google.gson.Gson
import com.lumos.api.ApiExecutor
import com.lumos.api.ExecutionApi
import com.lumos.api.RequestResult
import com.lumos.api.RequestResult.ServerError
import com.lumos.api.RequestResult.SuccessEmptyBody
import com.lumos.api.Update
import com.lumos.data.database.AppDatabase
import com.lumos.domain.model.DirectExecution
import com.lumos.domain.model.DirectExecutionDTOResponse
import com.lumos.domain.model.DirectExecutionStreet
import com.lumos.domain.model.DirectExecutionStreetItem
import com.lumos.domain.model.DirectReserve
import com.lumos.domain.model.InstallationView
import com.lumos.domain.model.ReserveMaterialJoin
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

    suspend fun checkUpdate(currentVersion: Long): RequestResult<Update> {

        val response = ApiExecutor.execute { api.checkUpdate(currentVersion) }
        return when (response) {
            is RequestResult.Success -> {
                RequestResult.Success(response.data)
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
                directExecutionId = executionDto.directExecutionId,
                executionStatus = "PENDING",
                type = "INSTALLATION",
                itemsQuantity = executionDto.reserves.size,
                creationDate = executionDto.creationDate,
                description = executionDto.description,
                instructions = executionDto.instructions,
                executorsIds = secureStorage.getOperationalUsers().toList()
            )

            db.directExecutionDao().insertExecution(execution)

            val reservations = executionDto.reserves.map { r ->
                DirectReserve(
                    reserveId = r.reserveId,
                    directExecutionId = executionDto.directExecutionId,
                    materialStockId = r.materialStockId,
                    contractItemId = r.contractItemId,
                    materialName = r.materialName,
                    materialQuantity = r.materialQuantity,
                    requestUnit = r.requestUnit
                )
            }

            db.directExecutionDao().insertReservations(reservations)
        }
    }


    fun getFlowDirectExecutions(): Flow<List<InstallationView>> =
        db.directExecutionDao().getFlowDirectExecutions()

    suspend fun getExecution(contractId: Long): DirectExecution? =
        db.directExecutionDao().getExecution(contractId)

    suspend fun getReservesOnce(directExecutionId: Long): List<ReserveMaterialJoin> =
        db.directExecutionDao().getReservesOnce(directExecutionId)

    private suspend fun debitMaterial(
        materialStockId: Long,
        contractId: Long,
        quantityExecuted: String
    ) {
        db.directExecutionDao().debitMaterial(materialStockId, contractId, quantityExecuted)
    }

    suspend fun createStreet(
        street: DirectExecutionStreet,
        items: List<DirectExecutionStreetItem>
    ) {
        db.withTransaction {
            val streetId = db.directExecutionDao().createStreet(street)
            if (streetId <= 0) {
                throw IllegalStateException("Endereço informado já enviado.")
            }

            for (item in items) {
                debitMaterial(
                    item.materialStockId,
                    item.contractItemId,
                    item.quantityExecuted
                )
                createStreetItem(
                    item.copy(
                        directStreetId = streetId
                    )
                )

                db.stockDao().debitStock(item.materialStockId, item.quantityExecuted)
            }

            queuePostDirectExecution(streetId)
            SyncManager.queueGetStock(
                context = app.applicationContext,
                db = db,
            )
        }
    }

    private suspend fun createStreetItem(item: DirectExecutionStreetItem) =
        db.directExecutionDao().insertDirectExecutionStreetItem(item)

    private suspend fun queuePostDirectExecution(streetId: Long) {
        SyncManager.queuePostDirectExecution(
            context = app.applicationContext,
            db = db,
            streetId = streetId
        )
    }

    suspend fun postDirectExecution(streetId: Long): RequestResult<Unit> {
        val gson = Gson()

        val photoUri = db.directExecutionDao().getPhotoUri(streetId)
            ?: return ServerError(-1, "Foto da pré-medição não encontrada")

        val street = db.directExecutionDao().getStreet(streetId)
        val materials = db.directExecutionDao().getStreetItems(streetId)


        val dto = SendDirectExecutionDto(
            directExecutionId = street.directExecutionId,
            description = street.description,
            deviceStreetId = street.directStreetId,
            deviceId = street.deviceId,
            latitude = street.latitude,
            longitude = street.longitude,
            address = street.address,
            lastPower = street.lastPower,
            materials = materials,
            currentSupply = street.currentSupply,
            finishAt = street.finishAt,
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

    suspend fun finishedDirectExecution(directExecutionId: Long): RequestResult<Unit> {
        val executorsIds = dao.directExecutionDao().getExecutorsIds(directExecutionId)
            .let { json -> Converters().toList(json) }
            .takeIf { it.isNotEmpty() } // só mantém se não estiver vazia
            ?: secureStorage.getOperationalUsers() // fallback

        val response =
            ApiExecutor.execute {
                api.finishDirectExecution(
                    directExecutionId = directExecutionId,
                    operationalUsers = executorsIds
                )
            }

        return when (response) {
            is RequestResult.Success -> {
                db.directExecutionDao().deleteDirectExecution(directExecutionId)
                db.directExecutionDao().deleteDirectReserves(directExecutionId)

                RequestResult.Success(Unit)
            }

            is SuccessEmptyBody -> {
                db.directExecutionDao().deleteDirectExecution(directExecutionId)
                db.directExecutionDao().deleteDirectReserves(directExecutionId)

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


    suspend fun markAsFinished(directExecutionId: Long) {
        db.directExecutionDao().markAsFinished(directExecutionId)
        SyncManager.markAsDirectExecutionAsFinished(
            context = app.applicationContext,
            db = db,
            directExecutionId = directExecutionId
        )
    }

    suspend fun countStock(): Int {
        return db.stockDao().materialCount()
    }

}