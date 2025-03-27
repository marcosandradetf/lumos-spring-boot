package com.lumos.data.repository

import com.lumos.data.api.ContractApi
import com.lumos.data.database.ContractDao
import com.lumos.domain.model.Contract
import retrofit2.HttpException

class ContractRepository(
    private val dao: ContractDao,
    private val api: ContractApi
) {

    suspend fun syncContracts() {
        val remoteContracts: List<Contract>

        try {
            val response = api.getContracts()
            if (response.isSuccessful) {
                val body = response.body()
                remoteContracts = body!!
                if (remoteContracts.isNotEmpty())
                    remoteContracts.forEach{ dao.insertContract(it) }

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

    suspend fun getContracts(status: String) : List<Contract> {
        return dao.getContracts(status)
    }

    suspend fun getContract(contractId: Long): Contract {
        return dao.getContract(contractId)
    }

    suspend fun setStatus(contractId: Long, status: String) {
        dao.setStatus(contractId, status)
    }

    suspend fun setDate(contractId: Long, updated: String) {
        dao.setDate(contractId, updated)
    }

}