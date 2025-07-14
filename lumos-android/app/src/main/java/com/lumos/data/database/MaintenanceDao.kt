package com.lumos.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.lumos.domain.model.Maintenance
import com.lumos.domain.model.MaintenanceStreet
import com.lumos.domain.model.MaintenanceStreetItem
import com.lumos.domain.model.MaintenanceStreetWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceDao {
    @Insert
    suspend fun insertMaintenance(maintenance: Maintenance)

    @Insert
    suspend fun insertMaintenanceStreet(street: MaintenanceStreet)

    @Insert
    suspend fun insertMaintenanceStreetItems(items: List<MaintenanceStreetItem>)

    @Query("select * from maintenance where status = :status")
    fun getFlowMaintenance(status: String): Flow<List<Maintenance>>

    @Query("select * from maintenancestreet where maintenanceId = :maintenanceId")
    fun getFlowStreets(maintenanceId: String): Flow<List<MaintenanceStreet>>

    @Query("select * from maintenance where maintenanceId = :maintenanceId")
    suspend fun getMaintenance(maintenanceId: String): Maintenance

    @Query("select * from maintenancestreet where maintenanceStreetId = :maintenanceStreetId")
    suspend fun getMaintenanceStreetWithItems(maintenanceStreetId: String): MaintenanceStreetWithItems

    @Query("select * from maintenanceStreetItem where maintenanceStreetId = :maintenanceStreetId")
    suspend fun getItemsByStreetId(maintenanceStreetId: String): List<MaintenanceStreetItem>

    @Transaction
    suspend fun deleteMaintenance(maintenanceId: String) {
        deleteMaintenanceById(maintenanceId)
        deleteStreetByMaintenanceId(maintenanceId)
        deleteStreetItemByMaintenanceId(maintenanceId)
    }

    @Query("delete from maintenance where maintenanceId = :maintenanceId")
    suspend fun deleteMaintenanceById(maintenanceId: String)

    @Query("delete from maintenancestreet where maintenanceId = :maintenanceId")
    suspend fun deleteStreetByMaintenanceId(maintenanceId: String)

    @Query("delete from maintenancestreetitem where maintenanceId = :maintenanceId")
    suspend fun deleteStreetItemByMaintenanceId(maintenanceId: String)

    @Query("select maintenanceId from maintenance where contractId = :contractId limit 1")
    suspend fun getMaintenanceIdByContractId(contractId: Long): String?


}
