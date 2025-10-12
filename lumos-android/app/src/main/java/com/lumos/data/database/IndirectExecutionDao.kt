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
    suspend fun insertPreMeasurementInstallation(installation: PreMeasurementInstallation)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStreets(streets: List<PreMeasurementInstallationStreet>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItems(items: List<PreMeasurementInstallationItem>)

    @Query("SELECT * FROM pre_measurement_installation WHERE preMeasurementId = :preMeasurementId")
    suspend fun getInstallationById(preMeasurementId: String): PreMeasurementInstallation?

    @Query("SELECT * FROM pre_measurement_installation")
    fun getAllInstallations(): Flow<List<PreMeasurementInstallation>>

    @Query("DELETE FROM pre_measurement_installation")
    suspend fun clearInstallations()

    @Query("DELETE FROM pre_measurement_installation_street")
    suspend fun clearStreets()

    @Query("DELETE FROM pre_measurement_installation_item")
    suspend fun clearItems()

    
}


