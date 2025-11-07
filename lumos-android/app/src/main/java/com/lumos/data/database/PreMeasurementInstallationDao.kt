package com.lumos.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lumos.domain.model.InstallationItemRequest
import com.lumos.repository.ExecutionStatus
import com.lumos.domain.model.InstallationView
import kotlinx.coroutines.flow.Flow
import com.lumos.domain.model.PreMeasurementInstallation
import com.lumos.domain.model.PreMeasurementInstallationItem
import com.lumos.domain.model.PreMeasurementInstallationStreet

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

    @Query("UPDATE PreMeasurementInstallation SET status = :status WHERE preMeasurementId = :id")
    suspend fun setInstallationStatus(id: String, status: String = ExecutionStatus.IN_PROGRESS)

    @Query("UPDATE PreMeasurementInstallationStreet SET status = :status WHERE preMeasurementStreetId = :id")
    suspend fun setStreetStatus(id: String, status: String = ExecutionStatus.IN_PROGRESS)

    @Query(
        """
        update premeasurementinstallationstreet 
        set installationPhotoUri = :photoUri 
        where preMeasurementStreetId = :streetId
    """
    )
    suspend fun setPhotoInstallationUri(photoUri: String, streetId: String)

    @Query("SELECT * from premeasurementinstallationstreet where preMeasurementId = :installationID")
    suspend fun getStreetsByInstallationId(installationID: String?): List<PreMeasurementInstallationStreet>

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
    suspend fun getPhotoUri(streetID: String): String

    @Query(
        """
            select 
                i.contractItemId as contractItemId,
                i.materialStockId as truckMaterialStockId,
                i.executedQuantity as executedQuantity,
                i.materialName as materialName
            from premeasurementinstallationitem i
            join premeasurementinstallationstreet s on s.preMeasurementStreetId = i.preMeasurementStreetId
            where s.preMeasurementStreetId = :streetId
        """
    )
    suspend fun getStreetItemsPayload(streetId: String): List<InstallationItemRequest>

}


