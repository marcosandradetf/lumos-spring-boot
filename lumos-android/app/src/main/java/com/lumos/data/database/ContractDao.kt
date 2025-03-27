package com.lumos.data.database

import androidx.room.*
import com.lumos.data.repository.Status
import com.lumos.domain.model.Contract


@Dao
interface ContractDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertContract(measurement: Contract): Long

    @Query("SELECT * FROM contracts WHERE status = :status")
    suspend fun getContracts(status: String): List<Contract>

    @Query("UPDATE contracts SET status = :status WHERE contractId = :contractId")
    suspend fun setStatus(contractId: Long, status: String)

    @Query("UPDATE contracts SET createdAt = :updated WHERE contractId = :contractId")
    suspend fun setDate(contractId: Long, updated: String)

    @Query("SELECT * FROM contracts WHERE contractId = :contractId")
    suspend fun getContract(contractId: Long): Contract

}