package com.lumos.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lumos.domain.model.Contract
import com.lumos.domain.model.Execution
import com.lumos.domain.model.Reserve
import kotlinx.coroutines.flow.Flow


@Dao
interface ExecutionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExecution(execution: Execution): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReserve(reserve: Reserve): Long

    @Query("SELECT * FROM reserves WHERE streetId = :streetId AND reserveStatus in (:status)")
    fun getFlowReserves(streetId: Long, status: List<String>): Flow<List<Reserve>>

    @Query("SELECT * FROM executions WHERE executionStatus <> 'FINISHED' ORDER BY priority DESC, creationDate ASC")
    fun getFlowExecutions(): Flow<List<Execution>>

    @Query("SELECT * FROM executions WHERE executionStatus = :status ORDER BY priority DESC, creationDate ASC")
    fun getFlowExecutions(status: String): Flow<List<Execution>>

//    @Query("SELECT * FROM contracts WHERE status = :status")
//    fun getFlowContracts(status: String): Flow<List<Contract>>

//    @Query("UPDATE contracts SET status = :status WHERE contractId = :contractId")
//    suspend fun setStatus(contractId: Long, status: String)
//
//    @Query("UPDATE contracts SET startAt = :updated, deviceId = :deviceId WHERE contractId = :contractId")
//    suspend fun startAt(contractId: Long, updated: String, deviceId: String)
//
//    @Query("SELECT * FROM contracts WHERE contractId = :contractId")
//    suspend fun getContract(contractId: Long): Contract
//
//    @Query("DELETE FROM contracts WHERE contractId = :contractId")
//    suspend fun deleteContract(contractId: Long)
}