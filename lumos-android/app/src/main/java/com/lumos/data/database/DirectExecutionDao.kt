package com.lumos.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.IGNORE
import androidx.room.Query
import com.lumos.domain.model.DirectExecution
import com.lumos.domain.model.DirectExecutionRequest
import com.lumos.domain.model.DirectExecutionStreet
import com.lumos.domain.model.DirectExecutionStreetItem
import com.lumos.domain.model.DirectReserve
import com.lumos.domain.model.ReserveMaterialJoin
import com.lumos.domain.model.ReservePartial

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

    @Query("select * from direct_execution where directExecutionId = :directExecutionId")
    suspend fun getExecution(directExecutionId: Long): DirectExecution?

    @Query(
        """
        SELECT de.reserveId as reserveId, de.directExecutionId as directExecutionId, de.materialStockId,
        de.contractItemId, de.materialName as materialName, de.materialQuantity as materialQuantity, 
        de.requestUnit as requestUnit,  ms.stockAvailable as stockAvailable, cib.currentBalance as currentBalance
        from direct_reserve de
        JOIN material_stock ms on ms.materialStockId = de.materialStockId
        LEFT JOIN ContractItemBalance cib on cib.contractItemId = de.contractItemId
        WHERE de.directExecutionId = :directExecutionId AND CAST(de.materialQuantity AS NUMERIC) > 0
    """
    )
    suspend fun getReservesOnce(directExecutionId: Long): List<ReserveMaterialJoin>

    @Insert(onConflict = IGNORE)
    suspend fun createStreet(street: DirectExecutionStreet): Long

    @Insert
    suspend fun insertDirectExecutionStreetItem(item: DirectExecutionStreetItem)

    @Query(
        """
        UPDATE direct_reserve
        SET materialQuantity = CAST(materialQuantity AS NUMERIC) - CAST(:quantityExecuted AS NUMERIC)
        WHERE materialStockId = :materialStockId
          AND contractItemId = :contractItemId
    """
    )
    suspend fun debitMaterial(
        materialStockId: Long,
        contractItemId: Long,
        quantityExecuted: String
    )


    @Query("select photoUri from direct_execution_street where directStreetId = :streetId")
    suspend fun getPhotoUri(streetId: Long): String?

    @Query(
        """
        SELECT reserveId, contractItemId, materialStockId as truckMaterialStockId, quantityExecuted, materialName
        FROM direct_execution_street_item 
        WHERE directStreetId = :streetId
    """
    )
    suspend fun getStreetItems(streetId: Long): List<ReservePartial>

    @Query(
        """
        SELECT *
        FROM direct_execution_street
        WHERE directStreetId = :streetId
    """
    )
    suspend fun getStreet(streetId: Long): DirectExecutionStreet

    @Query("DELETE FROM direct_execution_street WHERE directExecutionId = :directExecutionId")
    suspend fun deleteStreets(directExecutionId: Long)

    @Query(
        """
        DELETE FROM direct_execution_street_item 
        WHERE directStreetId IN (
            SELECT directStreetId
            FROM direct_execution_street
            WHERE directExecutionId = :directExecutionId
        )
    """
    )
    suspend fun deleteItems(directExecutionId: Long)

    @Query(
        """
        UPDATE direct_execution 
        SET executionStatus = :status 
        WHERE directExecutionId = :directExecutionId and executionStatus <> :status
    """
    )
    suspend fun setStatus(directExecutionId: Long, status: String = "FINISHED")

    @Query(
        """
        SELECT *
        FROM direct_execution_street
        WHERE directStreetId in (:streetIds)
    """
    )
    suspend fun getStreets(streetIds: List<Long>): List<DirectExecutionStreet>

    @Query(
        """
            SELECT 
                directExecutionId as directExecutionId,
                responsible as responsible,
                signPath as signPath,
                signDate as signDate,
                executorsIds as operationalUsers
            FROM direct_execution
            WHERE directExecutionId = :directExecutionId
        """
    )
    suspend fun getExecutionPayload(directExecutionId: Long): DirectExecutionRequest?

    @Query(
        """
        update direct_execution
        set responsible = :responsible,
            signPath = :signPath,
            signDate = :signDate
        where directExecutionId = :directExecutionId
    """
    )
    suspend fun markAsFinished(
        directExecutionId: Long,
        responsible: String?,
        signPath: String?,
        signDate: String?
    )

    @Query("SELECT * from direct_execution_street where directExecutionId = :installationID")
    suspend fun getStreetsByInstallationId(installationID: Long?): List<DirectExecutionStreet>
}