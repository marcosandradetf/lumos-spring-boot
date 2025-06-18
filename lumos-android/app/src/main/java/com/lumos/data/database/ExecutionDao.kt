package com.lumos.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lumos.data.repository.ExecutionStatus
import com.lumos.data.repository.ReservationStatus
import com.lumos.domain.model.Contract
import com.lumos.domain.model.DirectExecution
import com.lumos.domain.model.Execution
import com.lumos.domain.model.ExecutionHolder
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

    @Query("DELETE FROM direct_executions WHERE contractId = :contractId")
    suspend fun deleteDirectExecution(contractId: Long)

    @Query("DELETE FROM reserves WHERE streetId = :lng")
    suspend fun deleteReserves(lng: Long)

    @Query("DELETE FROM reserves WHERE contractId = :contractId")
    suspend fun deleteDirectReserves(contractId: Long)

    @Query("SELECT streetId, contractId, streetName, streetNumber, streetHood, city, state, executionStatus, priority, type, itemsQuantity, creationDate, latitude, longitude, photoUri, contractor, null" +
            " FROM executions WHERE executionStatus <> 'FINISHED' ORDER BY priority DESC, creationDate ASC")
    fun getFlowExecutions(): Flow<List<ExecutionHolder>>

    @Query("SELECT null, contractId, address, null, null, null, null, executionStatus, null, type, itemsQuantity, creationDate, latitude, longitude, photoUri, contractor, instructions" +
            " FROM direct_executions WHERE executionStatus <> 'FINISHED'")
    fun getFlowDirectExecutions(): Flow<List<ExecutionHolder>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDirectExecution(execution: DirectExecution)

}

