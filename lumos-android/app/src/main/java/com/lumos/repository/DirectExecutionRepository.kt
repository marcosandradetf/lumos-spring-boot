package com.lumos.repository

import android.app.Application
import android.util.Log
import androidx.core.net.toUri
import androidx.room.withTransaction
import com.google.gson.Gson
import com.lumos.api.ApiExecutor
import com.lumos.api.DirectExecutionApi
import com.lumos.api.RequestResult
import com.lumos.api.RequestResult.ServerError
import com.lumos.api.RequestResult.SuccessEmptyBody
import com.lumos.data.database.AppDatabase
import com.lumos.domain.model.ContractItemBalance
import com.lumos.domain.model.DirectExecution
import com.lumos.domain.model.DirectExecutionDTOResponse
import com.lumos.domain.model.DirectExecutionStreet
import com.lumos.domain.model.DirectExecutionStreetItem
import com.lumos.domain.model.DirectReserve
import com.lumos.domain.model.ReserveMaterialJoin
import com.lumos.domain.model.DirectExecutionStreetRequest
import com.lumos.midleware.SecureStorage
import com.lumos.utils.Utils
import com.lumos.utils.Utils.compressImageFromUri
import com.lumos.utils.Utils.getFileFromUri
import com.lumos.utils.Utils.isStaleCheckTeam
import com.lumos.worker.SyncManager
import com.lumos.worker.SyncTypes
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.collections.forEach

class DirectExecutionRepository(
    private val db: AppDatabase,
    private val api: DirectExecutionApi,
    private val secureStorage: SecureStorage,
    private val app: Application
) {

//    suspend fun checkUpdate(currentVersion: Long): RequestResult<Update> {
//
//        val response = ApiExecutor.execute { api.checkUpdate(currentVersion) }
//        return when (response) {
//            is RequestResult.Success -> {
//                RequestResult.Success(response.data)
//            }
//
//            is SuccessEmptyBody -> {
//                ServerError(204, "Resposta 204 inesperada")
//            }
//
//            is RequestResult.NoInternet -> {
//                SyncManager.queueSyncExecutions(app.applicationContext, db)
//                RequestResult.NoInternet
//            }
//
//            is RequestResult.Timeout -> RequestResult.Timeout
//            is ServerError -> ServerError(
//                response.code,
//                response.message
//            )
//
//            is RequestResult.UnknownError -> {
//                Log.e("Sync", "Erro desconhecido", response.error)
//                RequestResult.UnknownError(response.error)
//            }
//        }
//    }

    suspend fun syncDirectExecutions(): RequestResult<Unit> {
        val uuid = secureStorage.getUserUuid()
            ?: return ServerError(-1, "UUID Não encontrado")

        val executorsIds = secureStorage.getOperationalUsers().toList()
        if (executorsIds.isEmpty() || isStaleCheckTeam(secureStorage)) RequestResult.Success(Unit)

        val response = ApiExecutor.execute { api.getDirectExecutions(uuid) }
        return when (response) {
            is RequestResult.Success -> {
                saveDirectExecutionsToDb(response.data, executorsIds)
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

    private suspend fun saveDirectExecutionsToDb(fetchedExecutions: List<DirectExecutionDTOResponse>, executorsIds: List<String>) {
        fetchedExecutions.forEach { executionDto ->

            val execution = DirectExecution(
                directExecutionId = executionDto.directExecutionId,
                executionStatus = "PENDING",
                type = "INSTALLATION",
                itemsQuantity = executionDto.reserves.size,
                creationDate = executionDto.creationDate,
                description = executionDto.description,
                instructions = executionDto.instructions,
                executorsIds = executorsIds,
                contractId = executionDto.contractId,
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

            val items = executionDto.reserves.map { i ->
                ContractItemBalance(
                    contractItemId = i.contractItemId,
                    currentBalance = i.currentItemBalance,
                    itemName = i.currentItemName
                )
            }.toSet()

            db.directExecutionDao().insertReservations(reservations)
            db.contractDao().insertContractItemBalance(items.toList())
        }
    }

    suspend fun getReservesOnce(directExecutionId: Long): List<ReserveMaterialJoin> =
        db.directExecutionDao().getReservesOnce(directExecutionId)


    suspend fun saveAndQueueStreet(
        street: DirectExecutionStreet?,
        items: List<DirectExecutionStreetItem>
    ) {
        if (street == null) {
            throw Exception("parametro street null na fun createStreet")
        }
        db.withTransaction {
            val streetId = db.directExecutionDao().createStreet(street)
            if (streetId <= 0) {
                throw IllegalStateException("Endereço informado já enviado.")
            }

            for (item in items) {
                db.directExecutionDao().debitMaterial(
                    item.materialStockId, item.contractItemId, item.quantityExecuted
                )

                db.directExecutionDao().insertDirectExecutionStreetItem(
                    item.copy(
                        directStreetId = streetId
                    )
                )

                db.stockDao().debitStock(item.materialStockId, item.quantityExecuted)
                db.contractDao().debitContractItem(item.contractItemId, item.quantityExecuted)
            }

            SyncManager.queuePostDirectExecution(
                context = app.applicationContext,
                db = db,
                streetId = streetId
            )

            SyncManager.queueGetStock(app.applicationContext, db)
            SyncManager.queueContractItemBalance(app.applicationContext, db)
        }
    }

    suspend fun submitStreet(streetId: Long): RequestResult<Unit> {
        val gson = Gson()

        val photoUri = db.directExecutionDao().getPhotoUri(streetId)
            ?: return ServerError(-1, "Foto da pré-medição não encontrada")

        val street = db.directExecutionDao().getStreet(streetId)
        val materials = db.directExecutionDao().getStreetItems(streetId)


        val dto = DirectExecutionStreetRequest(
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
                    api.submitDirectExecutionStreet(
                        photo = imagePart,
                        execution = jsonBody
                    )
                }

            return when (response) {
                is RequestResult.Success -> {
                    Utils.deletePhoto(app.applicationContext, photoUri.toUri())
                    RequestResult.Success(Unit)
                }

                is SuccessEmptyBody -> {
                    Utils.deletePhoto(app.applicationContext, photoUri.toUri())
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

    suspend fun submitDirectExecution(directExecutionId: Long): RequestResult<Unit> {
        val gson = Gson()

        val installation = db.directExecutionDao().getExecutionPayload(directExecutionId)
        val signUri = installation?.signPath
        val executorsIds = installation?.operationalUsers
            ?.takeIf { it.isNotEmpty() }
            ?: secureStorage.getOperationalUsers().toList()

        val json = gson.toJson(installation?.copy(operationalUsers = executorsIds))
        val jsonBody = json.toRequestBody("application/json".toMediaType())

        val imagePart = signUri?.let {
            val file = getFileFromUri(
                app.applicationContext,
                it.toUri(),
                "signature_${System.currentTimeMillis()}.png"
            )
            val requestFile = file.asRequestBody("image/png".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("signature", file.name, requestFile)
        }

        val response = ApiExecutor.execute {
            api.submitDirectExecution(
                photo = imagePart,
                installation = jsonBody
            )
        }

        return when (response) {
            is RequestResult.Success -> {
                db.directExecutionDao().deleteDirectExecution(directExecutionId)
                db.directExecutionDao().deleteDirectReserves(directExecutionId)
                signUri?.let {
                    Utils.deletePhoto(app.applicationContext, it.toUri())
                }
                db.directExecutionDao().deleteStreets(directExecutionId)
                db.directExecutionDao().deleteItems(directExecutionId)

                RequestResult.Success(Unit)
            }

            is SuccessEmptyBody -> {
                db.directExecutionDao().deleteDirectExecution(directExecutionId)
                db.directExecutionDao().deleteDirectReserves(directExecutionId)
                signUri?.let {
                    Utils.deletePhoto(app.applicationContext, it.toUri())
                }
                db.directExecutionDao().deleteStreets(directExecutionId)
                db.directExecutionDao().deleteItems(directExecutionId)

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

    suspend fun setStatus(directExecutionId: Long, status: String = "FINISHED") {
        db.directExecutionDao().setStatus(directExecutionId, status)
    }

    suspend fun queueSubmitInstallation(
        directExecutionId: Long?,
        responsible: String?,
        signPath: String?,
        signDate: String?,
    ) {
        if (directExecutionId == null) {
            throw Exception("Parâmetro directExecutionId null durante a execução da queueSubmitInstallation")
        }

        db.directExecutionDao().markAsFinished(
            directExecutionId,
            responsible,
            signPath,
            signDate
        )
        SyncManager.queueSubmitInstallation(
            context = app.applicationContext,
            db = db,
            directExecutionId = directExecutionId
        )
    }

    suspend fun countStock(): Int {
        return db.stockDao().materialCount()
    }

    suspend fun getStreets(installationID: Long?): List<DirectExecutionStreet> {
        return db.directExecutionDao().getStreetsByInstallationId(installationID)
    }

}