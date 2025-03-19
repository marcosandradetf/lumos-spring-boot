package com.lumos.data.database

import androidx.room.*
import com.lumos.domain.model.Contract
import com.lumos.domain.model.Item

@Dao
interface ContractDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertContract(measurement: Contract): Long

    @Query("SELECT * FROM contracts WHERE status = 'PRE_MEASUREMENT_PROGRESS'")
    suspend fun getContracts(): List<Contract>

    @Query("UPDATE contracts SET status = 'PRE_MEASUREMENT_FINISHED' WHERE contractId = :contractId")
    suspend fun markAsMeasured(contractId: Long)
}