package com.lumos.data.database

import androidx.room.*
import com.lumos.data.repository.Status
import com.lumos.domain.model.PreMeasurementStreet
import com.lumos.domain.model.PreMeasurementStreetItem

@Dao
interface PreMeasurementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreet(preMeasurementStreet: PreMeasurementStreet): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(preMeasurementStreetItem: PreMeasurementStreetItem)

    @Query("SELECT * FROM pre_measurement_streets WHERE contractId = :preMeasurementId")
    suspend fun getStreets(preMeasurementId : Long): List<PreMeasurementStreet>

    @Query("SELECT * FROM pre_measurement_street_items WHERE contractId = :contractId")
    suspend fun getItems(contractId: Long): List<PreMeasurementStreetItem>

    @Query("DELETE FROM pre_measurement_streets WHERE contractId = :preMeasurementId")
    suspend fun deleteStreets(preMeasurementId: Long)

    @Query("DELETE FROM pre_measurement_street_items WHERE contractId = :contractId")
    suspend fun deleteItems(contractId: Long)
}