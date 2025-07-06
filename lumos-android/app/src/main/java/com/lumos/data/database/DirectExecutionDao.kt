package com.lumos.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.IGNORE
import androidx.room.Query
import androidx.room.Transaction
import com.lumos.domain.model.DirectExecution
import com.lumos.domain.model.DirectExecutionStreet
import com.lumos.domain.model.DirectExecutionStreetItem
import com.lumos.domain.model.DirectReserve
import com.lumos.domain.model.ExecutionHolder
import com.lumos.domain.model.ReservePartial
import kotlinx.coroutines.flow.Flow

@Dao
interface DirectExecutionDao {
    @Query(
        """
        UPDATE direct_reserve 
        SET materialQuantity = materialQuantity + :sumQuantity 
        WHERE materialStockId = :materialStockId AND contractItemId = :contractItemId
    """
    )
    suspend fun updateSumQuantity(
        materialStockId: Long,
        contractItemId: Long,
        sumQuantity: Double
    ): Int

    @Insert(onConflict = IGNORE)
    suspend fun insertReservations(reservation: List<DirectReserve>)

    @Insert(onConflict = IGNORE)
    suspend fun insertExecution(execution: DirectExecution)

    @Query("DELETE FROM direct_execution WHERE directExecutionId = :directExecutionId")
    suspend fun deleteDirectExecution(directExecutionId: Long)

    @Query("DELETE FROM direct_reserve WHERE directExecutionId = :directExecutionId")
    suspend fun deleteDirectReserves(directExecutionId: Long)

    @Query(
        "SELECT null, directExecutionId as contractId, null, null, null, null, null, executionStatus, null, type, itemsQuantity, creationDate, null, null, null, description as contractor, instructions" +
                " FROM direct_execution WHERE executionStatus <> 'FINISHED'"
    )
    fun getFlowDirectExecutions(): Flow<List<ExecutionHolder>>

    @Query("select * from direct_execution where directExecutionId = :directExecutionId")
    suspend fun getExecution(directExecutionId: Long): DirectExecution

    @Query("select * from direct_reserve where directExecutionId = :directExecutionId AND materialQuantity  > 0.0")
    suspend fun getReservesOnce(directExecutionId: Long): List<DirectReserve>

    @Insert(onConflict = IGNORE)
    suspend fun createStreet(street: DirectExecutionStreet): Long

    @Insert
    suspend fun insertDirectExecutionStreetItem(item: DirectExecutionStreetItem)

    @Query("""
        update direct_reserve
        set materialQuantity = materialQuantity - :quantityExecuted
        where materialStockId = :materialStockId 
        and contractItemId = :contractItemId
    """)
    suspend fun debitMaterial(materialStockId: Long, contractItemId: Long, quantityExecuted: Double)

    @Query("select photoUri from direct_execution_street where directStreetId = :streetId")
    suspend fun getPhotoUri(streetId: Long): String?

    @Query("""
        SELECT reserveId, contractItemId, materialStockId as truckMaterialStockId, quantityExecuted, materialName
        FROM direct_execution_street_item 
        WHERE directStreetId = :streetId
    """)
    suspend fun getStreetItems(streetId: Long): List<ReservePartial>

    @Query("""
        SELECT *
        FROM direct_execution_street
        WHERE directStreetId = :streetId
    """)
    suspend fun getStreet(streetId: Long): DirectExecutionStreet

    @Query("DELETE FROM direct_execution_street WHERE directStreetId = :streetId")
    suspend fun deleteStreet(streetId: Long)

    @Query("DELETE FROM direct_execution_street_item WHERE directStreetId = :streetId")
    suspend fun deleteItems(streetId: Long)

    @Query("UPDATE direct_execution set executionStatus = 'FINISHED' WHERE directExecutionId = :directExecutionId")
    suspend fun markAsFinished(directExecutionId: Long)



    @Query("""
        SELECT *
        FROM direct_execution_street
        WHERE directStreetId in (:streetIds)
    """)
    suspend fun getStreets(streetIds: List<Long>): List<DirectExecutionStreet>
}