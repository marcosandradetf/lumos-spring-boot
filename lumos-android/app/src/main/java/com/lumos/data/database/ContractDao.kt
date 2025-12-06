package com.lumos.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lumos.domain.model.Contract
import com.lumos.domain.model.ContractItemBalance
import com.lumos.domain.model.Item
import kotlinx.coroutines.flow.Flow


@Dao
interface ContractDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertContracts(contract: List<Contract>)

    @Query("DELETE FROM contracts")
    suspend fun deleteContracts()

    @Query("SELECT * FROM contracts WHERE status = :status and contractor not like '%manuten%' order by createdAt desc")
    fun getFlowContractsForPreMeasurement(status: String): Flow<List<Contract>>

    @Query("SELECT * FROM contracts WHERE status = :status order by createdAt desc")
    suspend fun getContracts(status: String): List<Contract>

    @Query("UPDATE contracts SET status = :status WHERE contractId = :contractId")
    suspend fun setStatus(contractId: Long, status: String)

    @Query("UPDATE contracts SET startAt = :updated, deviceId = :deviceId WHERE contractId = :contractId")
    suspend fun startAt(contractId: Long, updated: String, deviceId: String)

    @Query("SELECT * FROM contracts WHERE contractId = :contractId")
    suspend fun getContract(contractId: Long): Contract?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<Item>)

    @Query(
        """
        SELECT * FROM items 
        WHERE contractReferenceItemId IN (:itemsIds) AND type <> 'MANUTENÇÃO'
        order by description
    """
    )
    fun getItemsFromContract(itemsIds: List<Long>): Flow<List<Item>>

    @Query(
        """
        SELECT * FROM contracts 
        WHERE contractId IN (:longs)
        order by createdAt desc
    """
    )
    fun getFlowContractsByExecution(longs: List<Long>): Flow<List<Contract>>

    @Query("SELECT * FROM contracts WHERE hasMaintenance = 1 order by createdAt desc")
    fun getFlowContractsForMaintenance(): Flow<List<Contract>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContractItemBalance(contractItemBalance: List<ContractItemBalance>)

    @Query("UPDATE contractitembalance set currentBalance = CAST(currentBalance AS NUMERIC) - CAST(:quantityExecuted AS NUMERIC) WHERE contractItemId = :contractItemId")
    suspend fun debitContractItem(contractItemId: Long, quantityExecuted: String)

}