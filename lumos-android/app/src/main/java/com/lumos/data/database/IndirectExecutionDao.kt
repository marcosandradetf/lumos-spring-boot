package com.lumos.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lumos.repository.ExecutionStatus
import com.lumos.domain.model.ExecutionHolder
import com.lumos.domain.model.IndirectExecution
import com.lumos.domain.model.IndirectReserve
import com.lumos.domain.model.ReservePartial
import kotlinx.coroutines.flow.Flow


@Dao
interface IndirectExecutionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIndirectExecution(execution: IndirectExecution)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIndirectReserve(reserve: IndirectReserve)

    @Query("SELECT * FROM indirect_reserve WHERE streetId = :streetId AND quantityExecuted < materialQuantity")
    fun getFlowIndirectReserve(streetId: Long): Flow<List<IndirectReserve>>

    @Query("SELECT * FROM indirect_reserve WHERE streetId = :streetId AND quantityExecuted < materialQuantity")
    suspend fun getIndirectReserveOnce(streetId: Long): List<IndirectReserve>

    @Query("SELECT * FROM indirect_execution WHERE executionStatus = :status ORDER BY priority DESC, creationDate ASC")
    fun getFlowIndirectExecution(status: String): Flow<List<IndirectExecution>>

    @Query(
        "SELECT streetId, contractId, streetName, streetNumber, streetHood, city, state, executionStatus, priority, type, itemsQuantity, creationDate, latitude, longitude, photoUri, contractor, null" +
                " FROM indirect_execution WHERE executionStatus <> 'FINISHED' ORDER BY priority DESC, creationDate ASC"
    )
    fun getFlowIndirectExecution(): Flow<List<ExecutionHolder>>


    @Query("UPDATE indirect_execution SET executionStatus = :status WHERE streetId = :streetId")
    suspend fun setIndirectExecutionStatus(streetId: Long, status: String = ExecutionStatus.IN_PROGRESS)

    @Query("SELECT * FROM indirect_execution WHERE streetId = :lng LIMIT 1")
    suspend fun getExecution(lng: Long): IndirectExecution?

    @Query("UPDATE indirect_execution set photoUri = :photoUri where streetId = :streetId")
    suspend fun setIndirectExecutionPhotoUri(photoUri: String, streetId: Long)

    @Query("UPDATE indirect_reserve SET quantityExecuted = :quantityExecuted where reserveId = :reserveId")
    suspend fun finishMaterial(
        reserveId: Long,
        quantityExecuted: Double,
    )

    @Query("SELECT reserveId, contractItemId, 0 as truckMaterialStockId, quantityExecuted, materialName FROM indirect_reserve WHERE streetId = :lng")
    fun getReservesPartial(lng: Long): List<ReservePartial>

    @Query("SELECT photoUri FROM indirect_execution WHERE streetId = :lng LIMIT 1")
    suspend fun getPhotoUri(lng: Long): String?

    @Query("Select * from indirect_execution where contractId = (:lng) AND executionStatus in (:status)")
    suspend fun getExecutionsByContract(
        lng: Long,
        status: List<String> = listOf(
            ExecutionStatus.PENDING,
            ExecutionStatus.IN_PROGRESS
        ),
    ): List<IndirectExecution>

    @Query("DELETE FROM indirect_execution WHERE streetId = :lng")
    suspend fun deleteExecution(lng: Long)

    @Query("DELETE FROM indirect_execution WHERE streetId = :lng")
    suspend fun deleteReserves(lng: Long)


}


