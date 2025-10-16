package com.lumos.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    @Query("update premeasurementinstallationstreet set ph")
    suspend fun setPhotoInstallationUri(photoUri: String, streetId: String)

    
}


