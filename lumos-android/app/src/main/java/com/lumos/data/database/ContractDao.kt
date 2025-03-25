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
    suspend fun markAsMeasured(contractId: Long, status: String = Status.FINISHED)

    @Query("SELECT * FROM contracts WHERE contractId = :contractId")
    fun getContract(contractId: Long): Contract

}