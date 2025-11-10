package com.lumos.repository

import android.app.Application
import android.util.Log
import androidx.core.net.toUri
import androidx.room.withTransaction
import com.google.gson.Gson
import com.lumos.api.ApiExecutor
import com.lumos.api.ApiService
import com.lumos.api.MinioApi
import com.lumos.api.PreMeasurementInstallationApi
import com.lumos.api.RequestResult
import com.lumos.api.RequestResult.ServerError
import com.lumos.api.RequestResult.SuccessEmptyBody
import com.lumos.data.database.AppDatabase
import com.lumos.domain.model.InstallationRequest
import com.lumos.domain.model.ItemView
import com.lumos.domain.model.PreMeasurementInstallation
import com.lumos.domain.model.PreMeasurementInstallationItem
import com.lumos.domain.model.PreMeasurementInstallationStreet
import com.lumos.midleware.SecureStorage
import com.lumos.utils.Utils.compressImageFromUri
import com.lumos.worker.SyncManager
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class PreMeasurementInstallationRepository(
    private val db: AppDatabase,
    apiService: ApiService,
    private val secureStorage: SecureStorage,
    private val app: Application
) {
    private val api = apiService.createApi(PreMeasurementInstallationApi::class.java)
    private val minioApi = apiService.createApi(MinioApi::class.java)

    suspend fun syncExecutions(): RequestResult<Unit> {
        val response = ApiExecutor.execute { api.getExecutions("PENDING") }
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

    private suspend fun saveExecutionsToDb(fetchedExecutions: List<PreMeasurementInstallation>) {
        val installations = fetchedExecutions.map { installation ->
            PreMeasurementInstallation(
                preMeasurementId = installation.preMeasurementId,
                contractor = installation.contractor,
                instructions = installation.instructions,
                executorsIds = secureStorage.getOperationalUsers().toList()
            )
        }

        val streets = fetchedExecutions.flatMap { installation ->
            installation.streets.map { street ->
                PreMeasurementInstallationStreet(
                    preMeasurementStreetId = street.preMeasurementStreetId,
                    preMeasurementId = installation.preMeasurementId,
                    address = street.address,
                    priority = street.priority,
                    latitude = street.latitude,
                    longitude = street.longitude,
                    lastPower = street.lastPower,
                    photoUrl = street.photoUrl,
                    photoExpiration = street.photoExpiration,
                    objectUri = street.objectUri,
                    status = street.status,
                    installationPhotoUri = street.installationPhotoUri,
                )
            }
        }

        val items = fetchedExecutions.flatMap { installation ->
            installation.streets.flatMap { street ->
                street.items.map { item ->
                    PreMeasurementInstallationItem(
                        preMeasurementStreetId = street.preMeasurementStreetId,
                        materialStockId = item.materialStockId,
                        contractItemId = item.contractItemId,
                        materialName = item.materialName,
                        materialQuantity = item.materialQuantity,
                        requestUnit = item.requestUnit,
                        specs = item.specs
                    )
                }
            }
        }

        db.preMeasurementInstallationDao().insertInstallations(installations)
        db.preMeasurementInstallationDao().insertStreets(streets)
        db.preMeasurementInstallationDao().insertItems(items)
    }


    suspend fun setInstallationStatus(id: String, status: String = ExecutionStatus.IN_PROGRESS) {
        db.preMeasurementInstallationDao().setInstallationStatus(id, status)
    }

    suspend fun setStreetStatus(streetId: String, status: String = ExecutionStatus.IN_PROGRESS) {
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

    suspend fun setPhotoUri(photoUri: String, streetId: String) {
        db.preMeasurementInstallationDao().setPhotoInstallationUri(photoUri, streetId)
    }

    suspend fun postExecution(streetId: String): RequestResult<Unit> {
        val gson = Gson()

        val photoUri = db.preMeasurementInstallationDao().getPhotoUri(streetId)
        if (photoUri == null) {
            return ServerError(-1, "Foto da pré-medição não encontrada")
        }
        val items = db.preMeasurementInstallationDao().getStreetItemsPayload(streetId)
        val dto = InstallationRequest(
            streetId = streetId,
            items = items
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
                ApiExecutor.execute { api.uploadData(photo = imagePart, execution = jsonBody) }

            return when (response) {
                is RequestResult.Success -> {
                    db.preMeasurementInstallationDao().deleteInstallation(streetId)
                    db.preMeasurementInstallationDao().deleteItems(streetId)

                    RequestResult.Success(Unit)
                }

                is SuccessEmptyBody -> {
                    db.preMeasurementInstallationDao().deleteInstallation(streetId)
                    db.preMeasurementInstallationDao().deleteItems(streetId)

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

    suspend fun getStreets(installationID: String?, status: List<String> = listOf("PENDING", "IN_PROGRESS")): List<PreMeasurementInstallationStreet> {
        return db.preMeasurementInstallationDao().getStreetsByInstallationId(installationID, status)
    }

    suspend fun updateObjectPublicUrl(
        streetInstallationID: String,
        objectName: String
    ): RequestResult<Unit> {
        val response = ApiExecutor.execute { minioApi.updateObjectPublicUrl(objectName) }

        return when (response) {
            is RequestResult.Success -> {
                db.preMeasurementInstallationDao().updateObjectPublicUrl(
                    streetInstallationID,
                    response.data.newUrl,
                    response.data.expiryAt
                )
                RequestResult.Success(Unit)
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
            }
            db.preMeasurementInstallationDao().updateStreet(currentStreet.copy(status = "FINISHED"))
        }
        SyncManager.queueSubmitPreMeasurementInstallationStreet(
            app.applicationContext,
            db,
            currentStreetId
        )
    }

    suspend fun queueSubmitInstallation(installationID: String?, photoSignUri: String?, signDate: String?) {
        if (installationID == null) return
        db.preMeasurementInstallationDao().updateInstallation(installationID, photoSignUri, "FINISHED", signDate)

        SyncManager.queueSubmitPreMeasurementInstallationStreet(
            app.applicationContext,
            db,
            installationID
        )
    }

}