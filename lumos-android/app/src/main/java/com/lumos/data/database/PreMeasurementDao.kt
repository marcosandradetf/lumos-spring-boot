package com.lumos.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lumos.domain.model.PreMeasurement
import com.lumos.domain.model.PreMeasurementStreet
import com.lumos.domain.model.PreMeasurementStreetItem

@Dao
interface PreMeasurementDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMeasurement(preMeasurement: PreMeasurement)

    @Query("DELETE FROM pre_measurement WHERE preMeasurementId = :preMeasurementId")
    suspend fun deletePreMeasurement(preMeasurementId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStreet(preMeasurementStreet: PreMeasurementStreet)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItems(preMeasurementStreetItems: List<PreMeasurementStreetItem>)

    @Query("SELECT * FROM pre_measurement_street WHERE preMeasurementId = :preMeasurementId AND status = 'MEASURED'")
    suspend fun getStreets(preMeasurementId: String): List<PreMeasurementStreet>

    @Query("SELECT * FROM pre_measurement_street WHERE preMeasurementId = :preMeasurementId")
    suspend fun getAllStreets(preMeasurementId: String): List<PreMeasurementStreet>

    @Query("SELECT * FROM pre_measurement_street_item WHERE preMeasurementId = :preMeasurementId")
    suspend fun getItems(preMeasurementId: String): List<PreMeasurementStreetItem>

    @Query("DELETE FROM pre_measurement_street WHERE preMeasurementStreetId in (:preMeasurementStreetIds)")
    suspend fun deleteStreets(preMeasurementStreetIds: List<String>)

    @Query("DELETE FROM pre_measurement_street_item WHERE preMeasurementId = :preMeasurementId")
    suspend fun deleteItems(preMeasurementId: String)

    @Query("SELECT COUNT(*) from pre_measurement_street WHERE preMeasurementId = :preMeasurementId")
    suspend fun countPhotos(preMeasurementId: String): Int

    @Query("UPDATE pre_measurement_street SET status = 'FINISHED' WHERE preMeasurementId = (:preMeasurementId)")
    suspend fun finishAll(preMeasurementId: String)
}