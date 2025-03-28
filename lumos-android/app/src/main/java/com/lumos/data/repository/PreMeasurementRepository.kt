package com.lumos.data.repository

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.lumos.data.api.MeasurementApi
import com.lumos.data.api.PreMeasurementDto
import com.lumos.data.api.PreMeasurementStreetDto
import com.lumos.data.database.ContractDao
import com.lumos.data.database.PreMeasurementDao
import com.lumos.domain.model.Contract
import com.lumos.domain.model.PreMeasurementStreet
import com.lumos.domain.model.PreMeasurementStreetItem
import com.lumos.domain.service.SyncPreMeasurement
import java.util.concurrent.TimeUnit

class PreMeasurementRepository(
    private val preMeasurementDao: PreMeasurementDao,
    private val contractDao: ContractDao,
    private val api: MeasurementApi,
    private val context: Context
) {


    suspend fun saveStreet(preMeasurementStreet: PreMeasurementStreet): Long? {
        return try {
            preMeasurementDao.insertStreet(preMeasurementStreet)
        } catch (e: Exception) {
            Log.e("Error saveMeasurement", e.message.toString())
            null
        }
    }

    suspend fun getFinishedPreMeasurements(): List<Contract> {
        return contractDao.getContracts(Status.FINISHED)
    }

    suspend fun getPreMeasurement(contractId: Long): Contract {
        return contractDao.getContract(contractId)
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
            api.sendMeasurement(dto, userUuid)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun finishPreMeasurement(contractId: Long) {
        contractDao.deleteContract(contractId)
        preMeasurementDao.deleteStreets(contractId)
        preMeasurementDao.deleteItems(contractId)
    }

    suspend fun saveItem(preMeasurementStreetItem: PreMeasurementStreetItem) {
        preMeasurementDao.insertItem(preMeasurementStreetItem)
    }

    suspend fun getItems(contractId: Long): List<PreMeasurementStreetItem> {
        return preMeasurementDao.getItems(contractId)
    }

    fun syncMeasurement(contractId: Long) {
        // Agendar o Worker assim que a medição for adicionada
        val inputData = workDataOf("contract_id" to contractId) // Criando os parâmetros

        val workRequest = OneTimeWorkRequestBuilder<SyncPreMeasurement>()
            .setInputData(inputData) // Passando os dados
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30, TimeUnit.MINUTES
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(false)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "Sync $contractId", // Nome único para o trabalho
            ExistingWorkPolicy.REPLACE, // Pode substituir o trabalho se já estiver agendado
            workRequest
        )
    }

    suspend fun getStreets(contractId: Long): List<PreMeasurementStreet> {
        return preMeasurementDao.getStreets(contractId)
    }

}

object Status {
    const val PENDING = "PENDING"
    const val IN_PROGRESS = "IN_PROGRESS"
    const val FINISHED = "FINISHED"
}