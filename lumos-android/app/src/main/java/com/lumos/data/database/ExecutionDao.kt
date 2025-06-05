package com.lumos.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lumos.data.repository.ReservationStatus
import com.lumos.data.repository.Status
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

    @Query("SELECT * FROM executions WHERE executionStatus <> 'FINISHED' ORDER BY priority DESC, creationDate ASC")
    fun getFlowExecutions(): Flow<List<Execution>>

    @Query("SELECT * FROM executions WHERE executionStatus = :status ORDER BY priority DESC, creationDate ASC")
    fun getFlowExecutions(status: String): Flow<List<Execution>>

    @Query("UPDATE reserves SET reserveStatus = :status WHERE streetId = :streetId")
    suspend fun setReserveStatus(streetId: Long, status: String = ReservationStatus.COLLECTED)

    @Query("UPDATE executions SET executionStatus = :status WHERE streetId = :streetId")
    suspend fun setExecutionStatus(streetId: Long, status: String = Status.IN_PROGRESS)

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

}