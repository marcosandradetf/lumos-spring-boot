package com.lumos.repository

import android.app.Application
import android.util.Log
import androidx.core.net.toUri
import androidx.room.withTransaction
import com.lumos.api.ApiExecutor
import com.lumos.api.PreMeasurementApi
import com.lumos.api.PreMeasurementDto
import com.lumos.api.PreMeasurementStreetDto
import com.lumos.api.RequestResult
import com.lumos.data.database.AppDatabase
import com.lumos.domain.model.Contract
import com.lumos.domain.model.PreMeasurement
import com.lumos.domain.model.PreMeasurementStreet
import com.lumos.domain.model.PreMeasurementStreetItem
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

    suspend fun save(
        preMeasurementStreet: PreMeasurementStreet,
        items: List<PreMeasurementStreetItem>
    ) {
        try {
            db.withTransaction {
                db.preMeasurementDao().insertStreet(preMeasurementStreet)
                db.preMeasurementDao().insertItems(items)
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getFinishedPreMeasurements(): List<Contract> {
        return db.contractDao().getContracts(ExecutionStatus.FINISHED)
    }

    suspend fun getPreMeasurement(preMeasurementId: String): PreMeasurement {
        return db.preMeasurementDao().getPreMeasurementById(preMeasurementId)
    }

    suspend fun sendMeasurementToBackend(
        contractId: Long,
        preMeasurementStreet: List<PreMeasurementStreet>,
        preMeasurementStreetItems: List<PreMeasurementStreetItem>,
        userUuid: String,
    ): RequestResult<*> {
        if (preMeasurementStreetItems.isEmpty()) {
            return uploadStreetPhotos(TODO(), preMeasurementStreet)
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
                uploadStreetPhotos(TODO(), preMeasurementStreet)
            }

            is RequestResult.SuccessEmptyBody -> {
                uploadStreetPhotos(TODO(), preMeasurementStreet)
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

    private suspend fun finishPreMeasurement(preMeasurementId: String) {
        db.preMeasurementDao().deletePreMeasurement(preMeasurementId)
        db.preMeasurementDao().deleteItems(preMeasurementId)
    }

    suspend fun getItems(preMeasurementId: String): List<PreMeasurementStreetItem> {
        return db.preMeasurementDao().getItems(preMeasurementId)
    }

    suspend fun queueSendMeasurement(preMeasurementId: String) {
        db.preMeasurementDao().finishAll(preMeasurementId)
        SyncManager.queuePostPreMeasurement(
            app.applicationContext,
            db,
            preMeasurementId
        )
    }

    suspend fun getStreets(preMeasurementId: String): List<PreMeasurementStreet> {
        return db.preMeasurementDao().getStreets(preMeasurementId)
    }

    suspend fun getAllStreets(preMeasurementId: String): List<PreMeasurementStreet> {
        return db.preMeasurementDao().getAllStreets(preMeasurementId)
    }

    private suspend fun uploadStreetPhotos(
        preMeasurementId: String,
        streets: List<PreMeasurementStreet>
    ): RequestResult<*> {
        return try {
            finishPreMeasurement(preMeasurementId)

            Log.e("Enviando fotos", "Enviando fotos")

            val images = mutableListOf<MultipartBody.Part>()
            val streetsToDelete = mutableListOf<String>()
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
                                    "${street.preMeasurementStreetId}#${street.preMeasurementStreetId}#file.jpg",
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
                    db.preMeasurementDao().deleteStreets(streetsToDelete)
                    val count = db.preMeasurementDao().countPhotos(preMeasurementId).toString()
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

    suspend fun existsPreMeasurementByContractId(contractId: Long): PreMeasurement? {
        return db.preMeasurementDao().existsPreMeasurementByContractId(contractId)
    }

    suspend fun saveNewPreMeasurement(newPreMeasurement: PreMeasurement) {
        try {
            db.preMeasurementDao().insertMeasurement(newPreMeasurement)
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getPreMeasurements(): List<PreMeasurement> {
        try {
            return db.preMeasurementDao().getPreMeasurements()
        } catch (e: Exception) {
            throw e
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