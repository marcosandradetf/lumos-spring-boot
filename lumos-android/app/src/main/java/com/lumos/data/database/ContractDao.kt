package com.lumos.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lumos.domain.model.Contract
import kotlinx.coroutines.flow.Flow


@Dao
interface ContractDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertContract(measurement: Contract): Long

    @Query("SELECT * FROM contracts WHERE status = :status")
    fun getFlowContracts(status: String): Flow<List<Contract>>

    @Query("SELECT * FROM contracts WHERE status = :status")
    suspend fun getContracts(status: String): List<Contract>

    @Query("UPDATE contracts SET status = :status WHERE contractId = :contractId")
    suspend fun setStatus(contractId: Long, status: String)

    @Query("UPDATE contracts SET startAt = :updated, deviceId = :deviceId WHERE contractId = :contractId")
    suspend fun startAt(contractId: Long, updated: String, deviceId: String)

    @Query("SELECT * FROM contracts WHERE contractId = :contractId")
    suspend fun getContract(contractId: Long): Contract

    @Query("DELETE FROM contracts WHERE contractId = :contractId")
    suspend fun deleteContract(contractId: Long)
}