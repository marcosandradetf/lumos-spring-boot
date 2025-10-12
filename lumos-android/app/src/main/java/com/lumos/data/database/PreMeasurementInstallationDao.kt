package com.lumos.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lumos.repository.ExecutionStatus
import com.lumos.domain.model.ExecutionHolder
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

    @Query("SELECT * FROM pre_measurement_installation WHERE preMeasurementId = :preMeasurementId")
    suspend fun getInstallationById(preMeasurementId: String): PreMeasurementInstallation?

    @Query("""
        SELECT 
            pre_measurement_id as id, 
            'pre-measurement-installation' as type,
            0 as contractId,
            contractor as contractor,
            status as executionStatus,
            creationDate as creationDate,
            (
                SELECT COUNT(*) 
                FROM pre_measurement_installation_street 
                WHERE pre_measurement_id = pre_measurement_installation.preMeasurementId
            ) as streetsQuantity,
            (
                SELECT COUNT(*) 
                FROM pre_measurement_installation_street AS street
                JOIN pre_measurement_installation_item AS item
                    ON street.preMeasurementStreetId = item.preMeasurementStreetId
                WHERE street.preMeasurementId = pre_measurement_installation.preMeasurementId
            ) as itemsQuantity
        FROM pre_measurement_installation
    """)
    fun getInstallationsHolder(): Flow<List<ExecutionHolder>>

    @Query("SELECT * FROM pre_measurement_installation")
    fun getAllInstallations(): Flow<List<PreMeasurementInstallation>>

    @Query("DELETE FROM pre_measurement_installation")
    suspend fun clearInstallations()

    @Query("DELETE FROM pre_measurement_installation_street")
    suspend fun clearStreets()

    @Query("DELETE FROM pre_measurement_installation_item")
    suspend fun clearItems()

    @Query("UPDATE pre_measurement_installation SET status = :status WHERE preMeasurementId = :id")
    suspend fun setInstallationStatus(id: String, status: String = ExecutionStatus.IN_PROGRESS)

    @Query("UPDATE pre_measurement_installation_street SET status = :status WHERE preMeasurementStreetId = :id")
    suspend fun setStreetStatus(id: String, status: String = ExecutionStatus.IN_PROGRESS)

    
}


