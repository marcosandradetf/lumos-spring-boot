package com.lumos.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lumos.domain.model.InstallationItemRequest
import com.lumos.domain.model.InstallationRequest
import com.lumos.domain.model.ItemView
import com.lumos.domain.model.PreMeasurementInstallation
import com.lumos.domain.model.PreMeasurementInstallationItem
import com.lumos.domain.model.PreMeasurementInstallationStreet
import com.lumos.repository.ExecutionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface PreMeasurementInstallationDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInstallations(installations: List<PreMeasurementInstallation>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStreets(streets: List<PreMeasurementInstallationStreet>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItems(items: List<PreMeasurementInstallationItem>)

    @Query("SELECT * FROM PreMeasurementInstallation WHERE preMeasurementId = :preMeasurementId")
    suspend fun getInstallationById(preMeasurementId: String): PreMeasurementInstallation?

    @Query("SELECT * FROM PreMeasurementInstallation")
    fun getAllInstallations(): Flow<List<PreMeasurementInstallation>>

    @Query("DELETE FROM PreMeasurementInstallation")
    suspend fun clearInstallations()

    @Query("DELETE FROM PreMeasurementInstallationStreet")
    suspend fun clearStreets()

    @Query("DELETE FROM PreMeasurementInstallationItem")
    suspend fun clearItems()

    @Query("UPDATE PreMeasurementInstallation SET status = :status WHERE preMeasurementId = :id AND status <> :status")
    suspend fun setInstallationStatus(id: String, status: String = ExecutionStatus.IN_PROGRESS)

    @Query("UPDATE PreMeasurementInstallationStreet SET status = :status WHERE preMeasurementStreetId = :id AND status <> :status")
    suspend fun setStreetStatus(id: String, status: String = ExecutionStatus.IN_PROGRESS)

    @Query(
        """
        update premeasurementinstallationstreet 
        set installationPhotoUri = :photoUri 
        where preMeasurementStreetId = :streetId
    """
    )
    suspend fun setPhotoInstallationUri(photoUri: String, streetId: String)

    @Query("SELECT * from premeasurementinstallationstreet where preMeasurementId = :installationID and status in (:status)")
    suspend fun getStreetsByInstallationId(installationID: String?, status: List<String>): List<PreMeasurementInstallationStreet>

    @Query(
        """
        SELECT executorsIds
        FROM PreMeasurementInstallation
        WHERE preMeasurementId = :preMeasurementId
    """
    )
    suspend fun getExecutorsIds(preMeasurementId: String): String?

    @Query(
        """
        UPDATE PreMeasurementInstallationStreet
        SET photoUrl = :newUrl, photoExpiration = :newExpiration
        WHERE preMeasurementStreetId = :preMeasurementStreetId
    """
    )
    suspend fun updateObjectPublicUrl(
        preMeasurementStreetId: String,
        newUrl: String,
        newExpiration: Long
    )

    @Query(
        """
        select installationPhotoUri
        from PreMeasurementInstallationStreet
        where preMeasurementStreetId = :streetID
    """
    )
    suspend fun getPhotoUri(streetID: String): String?

    @Query(
        """
        select installationPhotoUri
        from PreMeasurementInstallationStreet
        where preMeasurementStreetId = :streetID
    """
    )
    suspend fun getSignPayLoad(streetID: String): String?

    @Query(
        """
            select 
                i.contractItemId as contractItemId,
                i.materialStockId as truckMaterialStockId,
                i.executedQuantity as quantityExecuted,
                i.materialName as materialName
            from premeasurementinstallationitem i
            join premeasurementinstallationstreet s on s.preMeasurementStreetId = i.preMeasurementStreetId
            where s.preMeasurementStreetId = :streetId
        """
    )
    suspend fun getStreetItemsPayload(streetId: String): List<InstallationItemRequest>

    @Query("DELETE from premeasurementinstallation where preMeasurementId = :preMeasurementId")
    suspend fun deleteInstallation(preMeasurementId: String)

    @Query("DELETE from premeasurementinstallationstreet where preMeasurementStreetId = :streetId")
    suspend fun deleteInstallationStreet(streetId: String)

    @Query("DELETE from premeasurementinstallationitem where preMeasurementStreetId = :streetId")
    suspend fun deleteItems(streetId: String)

    @Query("update premeasurementinstallation set status = 'FINISHED' where preMeasurementId = :preMeasurementInstallationId")
    suspend fun markAsFinished(preMeasurementInstallationId: String)

    @Query("""
        SELECT 
            i.preMeasurementStreetId as preMeasurementStreetId,
            i.materialStockId as materialStockId,
            i.contractItemId as contractItemId,
            i.materialName as materialName,
            i.materialQuantity as materialQuantity,
            i.requestUnit as requestUnit,
            i.specs as specs,
            s.stockQuantity as stockQuantity,
            i.executedQuantity as executedQuantity,
            c.currentBalance as currentBalance,
            c.itemName as itemName
        FROM premeasurementinstallationitem i
        JOIN material_stock s on s.materialStockId = i.materialStockId
        JOIN ContractItemBalance c on c.contractItemId = i.contractItemId
        WHERE preMeasurementStreetId = :preMeasurementStreetID
    """)
    suspend fun getItems(preMeasurementStreetID: String): List<ItemView>

    @Query("""
        UPDATE premeasurementinstallationitem
        SET executedQuantity = :quantityExecuted
        WHERE preMeasurementStreetId = :currentStreetId AND materialStockId = :materialStockId
    """)
    suspend fun setInstallationItemQuantity(
        currentStreetId: String?,
        materialStockId: Long,
        quantityExecuted: String
    )

    @Update
    suspend fun updateStreet(currentStreet: PreMeasurementInstallationStreet)

    @Query("""
        UPDATE premeasurementinstallation 
        SET signPath = :photoSignUri, status = :status, signDate = :signDate
        WHERE preMeasurementId = :installationID
    """)
    suspend fun updateInstallation(installationID: String, photoSignUri: String?, status: String, signDate: String?)

    @Query("""
        SELECT 
            preMeasurementId as installationId,
            responsible as responsible,
            signDate as signDate,
            signPath as signUri,
            executorsIds as operationalUsers
        FROM PreMeasurementInstallation
        WHERE preMeasurementId = :preMeasurementId
    """)
    suspend fun getInstallationRequest(preMeasurementId: String): InstallationRequest?


}


