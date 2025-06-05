package com.lumos.data.repository

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import com.lumos.data.api.ApiExecutor
import com.lumos.data.api.PreMeasurementApi
import com.lumos.data.api.PreMeasurementDto
import com.lumos.data.api.PreMeasurementStreetDto
import com.lumos.data.api.RequestResult
import com.lumos.data.database.AppDatabase
import com.lumos.domain.model.Contract
import com.lumos.domain.model.PreMeasurementStreet
import com.lumos.domain.model.PreMeasurementStreetItem
import com.lumos.domain.model.PreMeasurementStreetPhoto
import com.lumos.utils.Utils.compressImageFromUri
import com.lumos.worker.SyncManager
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class PreMeasurementRepository(
    private val db: AppDatabase,
    private val api: PreMeasurementApi,
    private val context: Context
) {

    suspend fun saveStreet(preMeasurementStreet: PreMeasurementStreet): Long? {
        return try {
            db.preMeasurementDao().insertStreet(preMeasurementStreet)
        } catch (e: Exception) {
            Log.e("Error saveMeasurement", e.message.toString())
            null
        }
    }

    suspend fun getFinishedPreMeasurements(): List<Contract> {
        return db.contractDao().getContracts(Status.FINISHED)
    }

    suspend fun getPreMeasurement(contractId: Long): Contract? {
        return db.contractDao().getContract(contractId)
    }

    suspend fun sendMeasurementToBackend(
        contractId: Long,
        preMeasurementStreet: List<PreMeasurementStreet>,
        preMeasurementStreetItems: List<PreMeasurementStreetItem>,
        userUuid: String,
        applicationContext: Context,
    ): RequestResult<*> {
        if(preMeasurementStreet.isEmpty()) {
            return uploadStreetPhotos(contractId, applicationContext)
        }

        val streets: MutableList<PreMeasurementStreetDto> = mutableListOf()
        val itemsByStreetId = preMeasurementStreetItems.groupBy { it.preMeasurementStreetId }

        preMeasurementStreet.forEach { street ->
            val items = itemsByStreetId[street.preMeasurementStreetId] ?: emptyList()
            streets.add(
                PreMeasurementStreetDto(
                    street = street,
                    items = items
                )
            )
        }

        val dto = PreMeasurementDto(
            contractId = contractId,
            streets = streets
        )

        Log.e("d", dto.toString())
        val response = ApiExecutor.execute { api.sendPreMeasurement(dto, userUuid) }
        return when (response) {
            is RequestResult.Success -> {
                uploadStreetPhotos(contractId, applicationContext)
            }

            is RequestResult.NoInternet -> {
                SyncManager.queueSyncContractItems(context, db)
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

    private suspend fun finishPreMeasurement(contractId: Long) {
        db.preMeasurementDao().deleteStreets(contractId)
        db.preMeasurementDao().deleteItems(contractId)
        db.contractDao().deleteContract(contractId)
    }

    suspend fun saveItem(preMeasurementStreetItem: PreMeasurementStreetItem) {
        db.preMeasurementDao().insertItem(preMeasurementStreetItem)
    }

    suspend fun saveStreetPhoto(photo: PreMeasurementStreetPhoto) {
        db.preMeasurementDao().insertPhoto(photo)
    }

    suspend fun getItems(contractId: Long): List<PreMeasurementStreetItem> {
        return db.preMeasurementDao().getItems(contractId)
    }

    suspend fun queueSendMeasurement(contractId: Long) {
        db.preMeasurementDao().finishAll(contractId)
        SyncManager.queuePostPreMeasurement(
            context,
            db,
            contractId
        )
    }

    suspend fun getStreets(contractId: Long): List<PreMeasurementStreet> {
        return db.preMeasurementDao().getStreets(contractId)
    }

    suspend fun getAllStreets(contractId: Long): List<PreMeasurementStreet> {
        return db.preMeasurementDao().getAllStreets(contractId)
    }

    private suspend fun uploadStreetPhotos(contractId: Long, context: Context): RequestResult<*> {
        finishPreMeasurement(
            contractId = contractId
        )

        Log.e("Enviando fotos","Enviando fotos")

        val images = mutableListOf<MultipartBody.Part>()
        val streets = db.preMeasurementDao().getStreetPhotos(contractId)
        val streetsToDelete = mutableListOf<Long>() // lista para armazenar IDs das fotos a deletar
        var quantity = 0

        for (street in streets) {
            if (quantity >= 6) break

            street.photoUri?.let {
                val bytes = compressImageFromUri(context, it.toUri())
                bytes?.let { byteArray ->
                    val requestFile = byteArray.toRequestBody("image/jpeg".toMediaType())
                    images.add(
                        MultipartBody.Part.createFormData(
                            "photos",
                            "${street.deviceId}#${street.preMeasurementStreetId}",
                            requestFile
                        )
                    )
                    quantity++
                    // Apenas adiciona o ID para deletar depois do upload bem-sucedido
                    streetsToDelete.add(street.preMeasurementStreetId)
                }
            }
        }

        if (images.isEmpty()) return RequestResult.ServerError(
            -1,
            "Imagens vazias"
        )

        val response = ApiExecutor.execute { api.uploadStreetPhotos(images) }
        return when (response) {
            is RequestResult.Success -> {
                db.preMeasurementDao().deletePhotos(streetsToDelete)
                val count = db.preMeasurementDao().countPhotos(contractId).toString()
                RequestResult.Success(count)
            }

            is RequestResult.NoInternet -> {
                SyncManager.queueSyncContractItems(context, db)
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


}

object Status {
    const val PENDING = "PENDING"
    const val REJECTED = "REJECTED"
    const val IN_PROGRESS = "IN_PROGRESS"
    const val FINISHED = "FINISHED"
}

object ReservationStatus {
    const val PENDING = "PENDING"
    const val APPROVED = "APPROVED"
    const val COLLECTED = "COLLECTED"
    const val CANCELLED = "CANCELLED"
    const val FINISHED = "FINISHED"
}