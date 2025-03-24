package com.lumos.data.database

import androidx.room.*
import com.lumos.domain.model.PreMeasurement
import com.lumos.domain.model.PreMeasurementStreetItem
import com.lumos.domain.model.PreMeasurementStreet
import com.lumos.domain.model.Status

@Dao
interface MeasurementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreMeasurement(preMeasurement: PreMeasurement): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreet(preMeasurementStreet: PreMeasurementStreet): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(preMeasurementStreetItem: PreMeasurementStreetItem)

    @Query("SELECT * FROM preMeasurements WHERE synced = 0")
    suspend fun getUnSyncedPreMeasurements(id : Long): List<PreMeasurement>

    @Query("SELECT * FROM preMeasurements WHERE status = :status")
    suspend fun getPreMeasurements(status: Status): List<PreMeasurement>

    @Query("SELECT * FROM preMeasurements WHERE preMeasurementId = :preMeasurementId")
    suspend fun getPreMeasurement(preMeasurementId: Long): PreMeasurement

    @Query("SELECT * FROM preMeasurementStreets WHERE preMeasurementId = :preMeasurementId")
    suspend fun getStreets(preMeasurementId : Long): List<PreMeasurementStreet>

    @Query("SELECT * FROM preMeasurementsStreetItems WHERE preMeasurementStreetId = :preMeasurementStreetId")
    suspend fun getItems(preMeasurementStreetId: Long): List<PreMeasurementStreetItem>

    @Query("UPDATE preMeasurements SET synced = 1 WHERE preMeasurementId = :preMeasurementId")
    suspend fun markAsSynced(preMeasurementId: Long)

}