package com.lumos.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.lumos.domain.model.Maintenance
import com.lumos.domain.model.MaintenanceJoin
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

    @Query("""
         SELECT 
            m.maintenanceId AS maintenanceId,
            m.contractId AS contractId,
            m.pendingPoints AS pendingPoints,
            m.quantityPendingPoints AS quantityPendingPoints,
            m.dateOfVisit AS dateOfVisit,
            m.type AS type,
            m.status AS status,
            c.contractor AS contractor
        FROM maintenance m
        JOIN contracts c ON c.contractId = m.contractId
        WHERE m.status = :status
    """)
    fun getFlowMaintenance(status: String): Flow<List<MaintenanceJoin>>

    @Query("""
        SELECT 
            m.maintenanceId AS maintenanceId,
            m.contractId AS contractId,
            m.pendingPoints AS pendingPoints,
            m.quantityPendingPoints AS quantityPendingPoints,
            m.dateOfVisit AS dateOfVisit,
            m.type AS type,
            m.status AS status,
            c.contractor AS contractor
        FROM maintenance m
        JOIN contracts c ON c.contractId = m.contractId
        WHERE m.status = :status
    """)
    suspend fun getMaintenancesByStatus(status: String): List<MaintenanceJoin>

    @Query("select * from maintenancestreet")
    fun getFlowStreets(): Flow<List<MaintenanceStreet>>

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

    @Update
    suspend fun updateMaintenance(maintenance: Maintenance)


}
