package com.lumos.data.database

import androidx.room.*
import com.lumos.data.entities.Measurement

@Dao
interface MeasurementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(measurement: Measurement)

    @Query("SELECT * FROM measurements WHERE synced = 0")
    suspend fun getUnsyncedMeasurements(): List<Measurement>

    @Query("UPDATE measurements SET synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)
}