package com.lumos.data.database

import androidx.room.*
import com.lumos.domain.model.Deposit
import com.lumos.domain.model.Material

@Dao
interface StockDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeposit(deposit: Deposit)

    @Query("SELECT * FROM deposits")
    suspend fun getDeposits(): List<Deposit>

    @Query("SELECT * FROM deposits WHERE regionName = :region LIMIT 1")
    suspend fun getDepositByRegion(region: String): Deposit?

    @Query("UPDATE deposits SET depositName = :depositName, regionName = :regionName, companyName = :companyName WHERE depositId = :id")
    suspend fun updateDeposit(id: Long, depositName: String, regionName: String, companyName: String)

    @Query("SELECT count(depositId) FROM deposits")
    suspend fun getCountDeposits(): Int

    @Query("DELETE FROM materials")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterial(material: Material)

    @Query("SELECT * FROM materials where depositId = :depositId ORDER BY materialPower")
    suspend fun getMaterials(depositId: Long): List<Material>

    @Query("SELECT count(materialId) FROM materials")
    suspend fun getCountMaterials(): Int

}