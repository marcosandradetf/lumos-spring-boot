package com.lumos.data.repository

import android.content.Context
import android.util.Log
import com.lumos.data.api.PreMeasurementApi
import com.lumos.data.api.PreMeasurementDto
import com.lumos.data.api.PreMeasurementStreetDto
import com.lumos.data.database.AppDatabase
import com.lumos.domain.model.Contract
import com.lumos.domain.model.PreMeasurementStreet
import com.lumos.domain.model.PreMeasurementStreetItem
import com.lumos.worker.SyncManager

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

    suspend fun getPreMeasurement(contractId: Long): Contract {
        return db.contractDao().getContract(contractId)
    }

    suspend fun sendMeasurementToBackend(
        contract: Contract,
        preMeasurementStreet: List<PreMeasurementStreet>,
        preMeasurementStreetItems: List<PreMeasurementStreetItem>,
        userUuid: String
    ): Boolean {
        return try {
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
                contractId = contract.contractId,
                streets = streets
            )
            val response = api.sendPreMeasurement(dto, userUuid)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun finishPreMeasurement(contractId: Long) {
        db.contractDao().deleteContract(contractId)
        db.preMeasurementDao().deleteStreets(contractId)
        db.preMeasurementDao().deleteItems(contractId)
    }

    suspend fun saveItem(preMeasurementStreetItem: PreMeasurementStreetItem) {
        db.preMeasurementDao().insertItem(preMeasurementStreetItem)
    }

    suspend fun getItems(contractId: Long): List<PreMeasurementStreetItem> {
        return db.preMeasurementDao().getItems(contractId)
    }

    suspend fun queueSyncMeasurement(contractId: Long) {
        SyncManager.queuePostPreMeasurement(
            context,
            db,
            contractId
        )
    }

    suspend fun getStreets(contractId: Long): List<PreMeasurementStreet> {
        return db.preMeasurementDao().getStreets(contractId)
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