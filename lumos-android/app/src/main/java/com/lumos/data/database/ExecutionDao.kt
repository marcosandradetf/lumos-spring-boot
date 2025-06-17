package com.lumos.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lumos.data.repository.ReservationStatus
import com.lumos.data.repository.ExecutionStatus
import com.lumos.domain.model.DirectExecution
import com.lumos.domain.model.Execution
import com.lumos.domain.model.Reserve
import kotlinx.coroutines.flow.Flow


@Dao
interface ExecutionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExecution(execution: Execution)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReserve(reserve: Reserve)

    @Query("SELECT * FROM reserves WHERE streetId = :streetId AND reserveStatus in (:status)")
    fun getFlowReserves(streetId: Long, status: List<String>): Flow<List<Reserve>>

    @Query("SELECT * FROM reserves WHERE streetId = :streetId AND reserveStatus in (:status)")
    suspend fun getReservesOnce(streetId: Long, status: List<String>): List<Reserve>

    @Query("SELECT * FROM executions WHERE executionStatus = :status ORDER BY priority DESC, creationDate ASC")
    fun getFlowExecutions(status: String): Flow<List<Execution>>

    @Query("UPDATE reserves SET reserveStatus = :status WHERE streetId = :streetId")
    suspend fun setReserveStatus(streetId: Long, status: String = ReservationStatus.COLLECTED)

    @Query("UPDATE executions SET executionStatus = :status WHERE streetId = :streetId")
    suspend fun setExecutionStatus(streetId: Long, status: String = ExecutionStatus.IN_PROGRESS)

    @Query("SELECT * FROM executions WHERE streetId = :lng LIMIT 1")
    suspend fun getExecution(lng: Long): Execution?

    @Query("UPDATE executions set photoUri = :photoUri where streetId = :streetId")
    suspend fun setPhotoUri(photoUri: String, streetId: Long)

    @Query("UPDATE reserves set reserveStatus = :status, quantityExecuted = :quantityExecuted where reserveId = :reserveId")
    suspend fun finishMaterial(
        reserveId: Long,
        quantityExecuted: Double,
        status: String = ReservationStatus.FINISHED
    )

    data class ReservePartial(
        val reserveId: Long,
        val quantityExecuted: Double,
    )
    @Query("SELECT streetId, reserveId, quantityExecuted FROM reserves WHERE streetId = :lng")
    fun getReservesPartial(lng: Long): List<ReservePartial>

    @Query("SELECT photoUri FROM executions WHERE streetId = :lng LIMIT 1")
    suspend fun getPhotoUri(lng: Long): String?

    @Query("Select * from executions where contractId = (:lng) AND executionStatus in (:status)")
    suspend fun getExecutionsByContract(
        lng: Long,
        status: List<String> = listOf(
            ExecutionStatus.PENDING,
            ExecutionStatus.IN_PROGRESS
        ),
    ): List<Execution>

    @Query("DELETE FROM executions WHERE streetId = :lng")
    suspend fun deleteExecution(lng: Long)

    @Query("DELETE FROM reserves WHERE streetId = :lng")
    suspend fun deleteReserves(lng: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDirectExecution(execution: DirectExecution)

    @Query("UPDATE direct_executions SET executionStatus = :status WHERE contractId = :contractId")
    suspend fun setDirectExecutionStatus(contractId: Long, status: String = ExecutionStatus.IN_PROGRESS)


    @Query("SELECT contractId, contractor  FROM executions WHERE executionStatus = :status ORDER BY priority DESC, creationDate ASC")
    fun getFlowExecutions(): Flow<List<ContractHolder>>

    @Query("SELECT contractId, contractor FROM direct_executions WHERE executionStatus <> 'FINISHED'")
    fun getFlowDirectExecutions(): Flow<List<ContractHolder>>

}

data class ContractHolder (
    val contractId: Long,
    val contractor: String
)
