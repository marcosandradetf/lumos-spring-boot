package com.lumos.data.database

import androidx.room.*
import com.lumos.domain.model.PreMeasurementStreet
import com.lumos.domain.model.PreMeasurementStreetItem
import com.lumos.domain.model.PreMeasurementStreetPhoto

@Dao
interface PreMeasurementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreet(preMeasurementStreet: PreMeasurementStreet): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(preMeasurementStreetItem: PreMeasurementStreetItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PreMeasurementStreetPhoto)

    @Query("SELECT * FROM pre_measurement_streets WHERE contractId = :preMeasurementId AND status = 'MEASURED'")
    suspend fun getStreets(preMeasurementId : Long): List<PreMeasurementStreet>

    @Query("SELECT * FROM pre_measurement_streets WHERE contractId = :preMeasurementId")
    suspend fun getAllStreets(preMeasurementId : Long): List<PreMeasurementStreet>

    @Query("SELECT * FROM pre_measurement_street_photos WHERE contractId = :contractId")
    suspend fun getStreetPhotos(contractId : Long): List<PreMeasurementStreetPhoto>

    @Query("SELECT * FROM pre_measurement_street_items WHERE contractId = :contractId")
    suspend fun getItems(contractId: Long): List<PreMeasurementStreetItem>

    @Query("DELETE FROM pre_measurement_streets WHERE contractId = :preMeasurementId")
    suspend fun deleteStreets(preMeasurementId: Long)

    @Query("DELETE FROM pre_measurement_street_items WHERE contractId = :contractId")
    suspend fun deleteItems(contractId: Long)

    @Query("SELECT COUNT(preMeasurementStreetId) from pre_measurement_street_photos WHERE contractId = :lng")
    suspend fun countPhotos(lng: Long): Int

    @Query("UPDATE pre_measurement_streets SET status = 'FINISHED' WHERE contractId = (:lng)")
    suspend fun finishAll(lng: Long)

    @Query("DELETE FROM pre_measurement_street_photos WHERE preMeasurementStreetId in (:longs)")
    suspend fun deletePhotos(longs: MutableList<Long>)

    @Query("select contractId from pre_measurement_streets")
    suspend fun getContractInUse(): List<Long>

}