package com.lumos.repository

import android.app.Application
import android.util.Log
import androidx.core.net.toUri
import androidx.room.withTransaction
import com.google.gson.Gson
import com.lumos.api.ApiExecutor
import com.lumos.api.MinioApi
import com.lumos.api.PreMeasurementInstallationApi
import com.lumos.api.RequestResult
import com.lumos.api.RequestResult.ServerError
import com.lumos.api.RequestResult.SuccessEmptyBody
import com.lumos.data.database.AppDatabase
import com.lumos.domain.model.ContractItemBalance
import com.lumos.domain.model.InstallationResponse
import com.lumos.domain.model.InstallationStreetRequest
import com.lumos.domain.model.ItemView
import com.lumos.domain.model.PreMeasurementInstallation
import com.lumos.domain.model.PreMeasurementInstallationItem
import com.lumos.domain.model.PreMeasurementInstallationStreet
import com.lumos.midleware.SecureStorage
import com.lumos.utils.Utils
import com.lumos.utils.Utils.compressImageFromUri
import com.lumos.utils.Utils.getFileFromUri
import com.lumos.utils.Utils.isStaleCheckTeam
import com.lumos.worker.SyncManager
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit

class PreMeasurementInstallationRepository(
    private val db: AppDatabase,
    retrofit: Retrofit,
    private val secureStorage: SecureStorage,
    private val app: Application
) {
    private val api = retrofit.create(PreMeasurementInstallationApi::class.java)
    private val minioApi = retrofit.create(MinioApi::class.java)

    suspend fun syncExecutions(): RequestResult<Unit> {
        val executorsIds = secureStorage.getOperationalUsers().toList()
        if (executorsIds.isEmpty() || isStaleCheckTeam(secureStorage)) RequestResult.Success(Unit)

        val response = ApiExecutor.execute { api.getInstallations("AVAILABLE_EXECUTION") }
        return when (response) {
            is RequestResult.Success -> {
                saveExecutionsToDb(response.data, executorsIds)
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

    private suspend fun saveExecutionsToDb(
        fetchedExecutions: List<InstallationResponse>,
        executorsIds: List<String>
    ) {
        val installations = fetchedExecutions.map {
            PreMeasurementInstallation(
                preMeasurementId = it.preMeasurementId,
                contractId = it.contractId,
                contractor = it.contractor,
                instructions = it.contractor,
                executorsIds = executorsIds
            )
        }
        Log.e("installations", installations.toString())


        val streets = fetchedExecutions.flatMap { installation ->
            installation.streets.map { street ->
                PreMeasurementInstallationStreet(
                    preMeasurementStreetId = street.preMeasurementStreetId,
                    preMeasurementId = street.preMeasurementId,
                    lastPower = street.lastPower,
                    latitude = street.latitude,
                    longitude = street.longitude,
                    address = street.address,
                    priority = street.priority,
                    photoUrl = street.photoUrl,
                    photoExpiration = street.photoExpiration,
                    objectUri = street.objectUri,
                )
            }
        }

        val items = fetchedExecutions.flatMap { installation ->
            installation.streets.flatMap { street ->
                street.items.map { item ->
                    PreMeasurementInstallationItem(
                        preMeasurementStreetId = item.preMeasurementStreetId,
                        materialStockId = item.materialStockId,
                        contractItemId = item.contractItemId,
                        materialName = item.materialName,
                        materialQuantity = item.materialQuantity,
                        requestUnit = item.requestUnit,
                        specs = null,
                    )
                }
            }
        }

        val contractItems = fetchedExecutions.flatMap { installation ->
            installation.streets.flatMap { street ->
                street.items.map { item ->
                    ContractItemBalance(
                        contractItemId = item.contractItemId,
                        currentBalance = item.currentBalance,
                        itemName = item.itemName
                    )
                }
            }
        }.toSet()

        db.preMeasurementInstallationDao().insertInstallations(installations)
        db.preMeasurementInstallationDao().insertStreets(streets)
        db.preMeasurementInstallationDao().insertItems(items)
        db.contractDao().insertContractItemBalance(contractItems.toList())
    }


    suspend fun setInstallationStatus(id: String, status: String = ExecutionStatus.IN_PROGRESS) {
        db.preMeasurementInstallationDao().setInstallationStatus(id, status)
    }

    suspend fun setStreetStatus(streetId: String, status: String = ExecutionStatus.IN_PROGRESS) {
        println("setStreetStatus")
        db.preMeasurementInstallationDao().setStreetStatus(streetId, status)
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

    suspend fun submitStreet(streetId: String): RequestResult<Unit> {
        val gson = Gson()

        val payload = db.preMeasurementInstallationDao().getInstallationStreetPayload(streetId)
        val photoUri = payload?.installationPhotoUri
        val items = db.preMeasurementInstallationDao().getStreetItemsPayload(streetId)

        val dto = InstallationStreetRequest(
            streetId = streetId,
            currentSupply = payload?.currentSupply,
            lastPower = payload?.lastPower,
            latitude = payload?.latitude,
            longitude = payload?.longitude,
            items = items
        )

        val json = gson.toJson(dto)
        val jsonBody = json.toRequestBody("application/json".toMediaType())

        val byteArray = photoUri?.let { compressImageFromUri(app.applicationContext, it.toUri()) }
        val imagePart = byteArray?.let {
            val requestFile = it.toRequestBody("image/jpeg".toMediaType())
            MultipartBody.Part.createFormData(
                "photo",
                "upload_${System.currentTimeMillis()}.jpg",
                requestFile
            )
        }

        val response = ApiExecutor.execute {
            api.submitInstallationStreet(
                photo = imagePart,
                installationStreet = jsonBody
            )
        }
        return when (response) {
            is RequestResult.Success -> {
                db.preMeasurementInstallationDao().deleteInstallationStreet(streetId)
                db.preMeasurementInstallationDao().deleteItems(streetId)
                photoUri?.let {
                    Utils.deletePhoto(app.applicationContext, it.toUri())
                }
                RequestResult.Success(Unit)
            }

            is SuccessEmptyBody -> {
                db.preMeasurementInstallationDao().deleteInstallationStreet(streetId)
                db.preMeasurementInstallationDao().deleteItems(streetId)
                photoUri?.let {
                    Utils.deletePhoto(app.applicationContext, it.toUri())
                }
                SuccessEmptyBody
            }

            is RequestResult.NoInternet -> RequestResult.NoInternet
            is RequestResult.Timeout -> RequestResult.Timeout
            is ServerError -> ServerError(response.code, response.message)
            is RequestResult.UnknownError -> RequestResult.UnknownError(response.error)
        }
    }

    suspend fun submitInstallation(installationId: String): RequestResult<Unit> {
        val gson = Gson()

        val payload = db.preMeasurementInstallationDao().getInstallationRequest(installationId)
        if (payload == null) {
            return RequestResult.UnknownError(Exception("payload vazio na fun submitInstallation"))
        }

        val json = gson.toJson(payload)
        val jsonBody = json.toRequestBody("application/json".toMediaType())

        val imagePart = payload.signUri?.let  {
            val file = getFileFromUri(
                app.applicationContext,
                it.toUri(),
                "signature_${System.currentTimeMillis()}.png"
            )
            val requestFile = file.asRequestBody("image/png".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("signature", file.name, requestFile)
        }

        val response = ApiExecutor.execute {
            api.submitInstallation(
                signature = imagePart,
                installation = jsonBody
            )
        }

        return when (response) {
            is RequestResult.Success, SuccessEmptyBody -> {
                db.preMeasurementInstallationDao().deleteInstallation(installationId)
                payload.signUri?.let {
                    Utils.deletePhoto(app.applicationContext, it.toUri())
                }
                RequestResult.Success(Unit)
            }

            is RequestResult.NoInternet -> RequestResult.NoInternet
            is RequestResult.Timeout -> RequestResult.Timeout
            is ServerError -> ServerError(response.code, response.message)
            is RequestResult.UnknownError -> RequestResult.UnknownError(response.error)
        }
    }

    suspend fun getStreets(
        installationID: String?,
        status: List<String> = listOf("PENDING", "IN_PROGRESS")
    ): List<PreMeasurementInstallationStreet> {
        return db.preMeasurementInstallationDao().getStreetsByInstallationId(installationID, status)
    }

    suspend fun updateObjectPublicUrl(
        streetInstallationID: String,
        objectName: String
    ): RequestResult<String> {
        val response = ApiExecutor.execute { minioApi.updateObjectPublicUrl(objectName) }

        return when (response) {
            is RequestResult.Success -> {
                db.preMeasurementInstallationDao().updateObjectPublicUrl(
                    streetInstallationID,
                    response.data.newUrl,
                    response.data.expiryAt
                )
                RequestResult.Success(response.data.newUrl)
            }

            is SuccessEmptyBody -> {
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

    suspend fun getItems(preMeasurementStreetID: String): List<ItemView> {
        return db.preMeasurementInstallationDao().getItems(preMeasurementStreetID)
    }

    suspend fun setInstallationItemQuantity(
        currentStreetId: String?,
        materialStockId: Long,
        quantityExecuted: String
    ) {
        db.preMeasurementInstallationDao()
            .setInstallationItemQuantity(currentStreetId, materialStockId, quantityExecuted)
    }

    suspend fun queueSubmitStreet(currentStreet: PreMeasurementInstallationStreet?) {
        if (currentStreet == null) return
        val currentStreetId = currentStreet.preMeasurementStreetId
        val items = db.preMeasurementInstallationDao().getItems(currentStreetId)
        db.withTransaction {
            items.forEach {
                db.stockDao().debitStock(it.materialStockId, it.executedQuantity)
                db.contractDao().debitContractItem(it.contractItemId, it.executedQuantity)
            }
            db.preMeasurementInstallationDao().updateStreet(currentStreet)
        }

        SyncManager.queueSubmitPreMeasurementInstallationStreet(
            app.applicationContext,
            db,
            currentStreetId
        )
    }

    suspend fun queueSubmitInstallation(
        installationID: String?,
        photoSignUri: String?,
        signDate: String?
    ) {
        if (installationID == null) return
        db.preMeasurementInstallationDao()
            .updateInstallation(installationID, photoSignUri, "FINISHED", signDate)

        SyncManager.queueSubmitPreMeasurementInstallation(
            app.applicationContext,
            db,
            installationID
        )
    }

}