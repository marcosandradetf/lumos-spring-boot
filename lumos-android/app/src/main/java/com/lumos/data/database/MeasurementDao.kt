package com.lumos.data.database

import androidx.room.*
import com.lumos.domain.model.Item
import com.lumos.domain.model.Measurement

@Dao
interface MeasurementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurement(measurement: Measurement): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: Item)

    @Query("SELECT * FROM measurements WHERE synced = 0")
    suspend fun getUnsyncedMeasurements(): List<Measurement>

    @Query("UPDATE measurements SET synced = 1 WHERE measurementId = :measurementId")
    suspend fun markAsSynced(measurementId: Long)

    @Query("SELECT * FROM items WHERE measurementId = :measurementId")
    suspend fun getItems(measurementId: Long): List<Item>

//    @Query("SELECT * FROM measurements m INNER JOIN items i on i.measurementId = m.measurementId WHERE m.synced = 0")
//    suspend fun getUnsyncedMeasurementsWithRelationship(): List<MeasurementWithItems>

}