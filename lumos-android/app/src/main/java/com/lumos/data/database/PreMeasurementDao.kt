package com.lumos.data.database

import androidx.room.*
import com.lumos.data.repository.Status
import com.lumos.domain.model.PreMeasurement
import com.lumos.domain.model.PreMeasurementStreet
import com.lumos.domain.model.PreMeasurementStreetItem

@Dao
interface PreMeasurementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreMeasurement(preMeasurement: PreMeasurement): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreet(preMeasurementStreet: PreMeasurementStreet): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(preMeasurementStreetItem: PreMeasurementStreetItem)

    @Query("SELECT * FROM pre_measurements WHERE synced = 0 and status = :status")
    suspend fun getUnSyncedPreMeasurements(status : String = Status.FINISHED): List<PreMeasurement>

    @Query("SELECT * FROM pre_measurements WHERE status = :status")
    suspend fun getPreMeasurements(status: String): List<PreMeasurement>

    @Query("SELECT * FROM pre_measurements WHERE preMeasurementId = :preMeasurementId")
    suspend fun getPreMeasurement(preMeasurementId: Long): PreMeasurement

    @Query("SELECT * FROM pre_measurement_streets WHERE preMeasurementId = :preMeasurementId")
    suspend fun getStreets(preMeasurementId : Long): List<PreMeasurementStreet>

    @Query("SELECT * FROM pre_measurement_street_items WHERE preMeasurementStreetId = :preMeasurementStreetId")
    suspend fun getItems(preMeasurementStreetId: Long): List<PreMeasurementStreetItem>

    @Query("UPDATE pre_measurements SET synced = 1 WHERE preMeasurementId = :preMeasurementId")
    suspend fun markAsSynced(preMeasurementId: Long)

}