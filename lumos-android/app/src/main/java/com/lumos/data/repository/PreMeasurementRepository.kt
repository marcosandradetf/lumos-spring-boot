package com.lumos.data.repository

import android.app.Application
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
    private val app: Application
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
        return db.contractDao().getContracts(ExecutionStatus.FINISHED)
    }

    suspend fun getPreMeasurement(contractId: Long): Contract? {
        return db.contractDao().getContract(contractId)
    }

    suspend fun sendMeasurementToBackend(
        contractId: Long,
        preMeasurementStreet: List<PreMeasurementStreet>,
        preMeasurementStreetItems: List<PreMeasurementStreetItem>,
        userUuid: String,
    ): RequestResult<*> {
        if(preMeasurementStreet.isEmpty()) {
            return uploadStreetPhotos(contractId)
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
                uploadStreetPhotos(contractId)
            }

            is RequestResult.SuccessEmptyBody -> {
                uploadStreetPhotos(contractId)
            }

            is RequestResult.NoInternet -> {
                SyncManager.queueSyncContractItems(app.applicationContext, db)
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
            app.applicationContext,
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

    private suspend fun uploadStreetPhotos(contractId: Long): RequestResult<*> {
        return try {
            finishPreMeasurement(contractId)

            Log.e("Enviando fotos", "Enviando fotos")

            val images = mutableListOf<MultipartBody.Part>()
            val streets = db.preMeasurementDao().getStreetPhotos(contractId)
            val streetsToDelete = mutableListOf<Long>()
            var quantity = 0

            for (street in streets) {
                if (quantity >= 5) break

                street.photoUri?.let {
                    try {
                        val bytes = compressImageFromUri(app.applicationContext, it.toUri())
                        bytes?.let { byteArray ->
                            val requestFile = byteArray.toRequestBody("image/jpeg".toMediaType())
                            images.add(
                                MultipartBody.Part.createFormData(
                                    "photos",
                                    "${street.deviceId}#${street.preMeasurementStreetId}#file.jpg",
                                    requestFile
                                )
                            )
                            quantity++
                            streetsToDelete.add(street.preMeasurementStreetId)
                        }
                    } catch (e: Exception) {
                        Log.e("Upload", "Erro ao processar imagem: $it", e)
                    }
                }
            }


            Log.e("quantity", images.size.toString())

            if (images.isEmpty()) return RequestResult.Success(0)

            val response = ApiExecutor.execute { api.uploadStreetPhotos(images) }

            when (response) {
                is RequestResult.Success,
                is RequestResult.SuccessEmptyBody -> {
                    db.preMeasurementDao().deletePhotos(streetsToDelete)
                    val count = db.preMeasurementDao().countPhotos(contractId).toString()
                    RequestResult.Success(count)
                }

                is RequestResult.NoInternet -> {
                    SyncManager.queueSyncContractItems(app.applicationContext, db)
                    RequestResult.NoInternet
                }

                is RequestResult.Timeout -> response
                is RequestResult.ServerError -> {
                    Log.e("Upload", "Erro do servidor: ${response.code} - ${response.message}")
                    response
                }

                is RequestResult.UnknownError -> {
                    Log.e("Upload", "Erro desconhecido: ${response.error?.message}", response.error)
                    response
                }
            }
        } catch (e: Exception) {
            Log.e("Upload", "Falha geral no upload", e)
            RequestResult.UnknownError(e)
        }
    }



}

object ContractStatus {
    const val ACTIVE = "ACTIVE"
    const val INACTIVE = "INACTIVE"
    const val ARCHIVED = "ARCHIVED"
}

object ExecutionStatus {
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