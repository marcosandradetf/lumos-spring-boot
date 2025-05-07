package com.lumos.data.repository

import com.lumos.data.api.ContractApi
import com.lumos.data.api.ExecutionApi
import com.lumos.data.database.ContractDao
import com.lumos.data.database.ExecutionDao
import com.lumos.domain.model.Contract
import com.lumos.domain.model.Execution
import com.lumos.domain.model.ExecutionDTO
import com.lumos.domain.model.Reserve
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import java.util.UUID

class ExecutionRepository(
    private val dao: ExecutionDao,
    private val api: ExecutionApi
) {

    suspend fun syncExecutions() {
        val remoteExecutions: List<ExecutionDTO>

        try {
            val response = api.getExecutions(UUID.fromString(""))
            if (response.isSuccessful) {
                val body = response.body()
                remoteExecutions = body!!
                if (remoteExecutions.isNotEmpty())
                    remoteExecutions.forEach { it ->
                        val streetId = it.streetId
                        dao.insertExecution(
                            Execution(
                                streetId = streetId,
                                streetName = it.streetName,
                                teamId = it.teamId,
                                teamName = it.teamName,
                                executionStatus = "PENDING",
                                priority = it.priority,
                                type = it.type,
                                itemsQuantity =it.itemsQuantity,
                                creationDate = it.creationDate
                            )
                        )
                        it.reserves.forEach { r ->
                            dao.insertReserve(
                                Reserve(
                                    reserveId = r.reserveId,
                                    materialId = r.materialId,
                                    materialName = r.materialName,
                                    materialQuantity = r.materialQuantity,
                                    reserveStatus = r.reserveStatus,
                                    streetId = streetId
                                )
                            )
                        }
                    }

            } else {
                val code = response.code()
                // TODO handle the error
            }
        } catch (e: HttpException) {
            val response = e.response()
            val errorCode = e.code()
            // TODO handle the error
        }

    }

    fun getFlowExecutions(): Flow<List<Execution>> =
        dao.getFlowExecutions()

    fun getFlowReserves(streetId: Long, status: List<String>): Flow<List<Reserve>> =
        dao.getFlowReserves(streetId, status)


//    fun getFlowContracts(status: String): Flow<List<Contract>> =
//        dao.getFlowContracts(status)
//
//
//    suspend fun getContract(contractId: Long): Contract {
//        return dao.getContract(contractId)
//    }
//
//    suspend fun setStatus(contractId: Long, status: String) {
//        dao.setStatus(contractId, status)
//    }
//
//    suspend fun startAt(contractId: Long, updated: String, deviceId: String) {
//        dao.startAt(contractId, updated, deviceId)
//    }

}