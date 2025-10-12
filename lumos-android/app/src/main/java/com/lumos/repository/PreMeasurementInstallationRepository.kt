package com.lumos.repository

import android.app.Application
import android.util.Log
import androidx.core.net.toUri
import com.google.gson.Gson
import com.lumos.api.ApiExecutor
import com.lumos.api.ExecutionApi
import com.lumos.api.RequestResult
import com.lumos.api.RequestResult.ServerError
import com.lumos.api.RequestResult.SuccessEmptyBody
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
import com.lumos.domain.model.PreMeasurementInstallation
import com.lumos.domain.model.PreMeasurementInstallationItem
import com.lumos.domain.model.PreMeasurementInstallationStreet

class PreMeasurementInstallationRepository(
    private val db: AppDatabase,
    private val api: ExecutionApi,
    private val secureStorage: SecureStorage,
    private val app: Application
) {

    suspend fun syncExecutions(): RequestResult<Unit> {
        val response = ApiExecutor.execute { api.getExecutions() }
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
                streets = emptyList()
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
                    items = emptyList()
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

    fun getFlowExecutions(): Flow<List<ExecutionHolder>> =
        db.preMeasurementInstallationDao().getInstallationsHolder()

    fun getFlowReserves(streetId: Long): Flow<List<IndirectReserve>> =
        db.preMeasurementInstallationDao().getFlowIndirectReserve(streetId)

    suspend fun setInstallationStatus(id: String, status: String = ExecutionStatus.IN_PROGRESS) {
        db.preMeasurementInstallationDao().setInstallationStatus(id, status)
    }

    suspend fun setExecutionStatus(streetId: String, status: String = ExecutionStatus.IN_PROGRESS) {
        db.preMeasurementInstallationDao().setStreetStatus(streetId, status)
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
        db.preMeasurementInstallationDao().setIndirectExecutionPhotoUri(photoUri, streetId)
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

        val photoUri = db.preMeasurementInstallationDao().getPhotoUri(streetId)
        if(photoUri == null) {
            return ServerError(-1, "Foto da pré-medição não encontrada")
        }
        val reserves = db.preMeasurementInstallationDao().getReservesPartial(streetId)
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
                    db.preMeasurementInstallationDao().deleteExecution(streetId)
                    db.preMeasurementInstallationDao().deleteReserves(streetId)

                    RequestResult.Success(Unit)
                }
                is SuccessEmptyBody -> {
                    db.preMeasurementInstallationDao().deleteExecution(streetId)
                    db.preMeasurementInstallationDao().deleteReserves(streetId)

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

}