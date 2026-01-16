package com.lumos.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.lumos.domain.model.Deposit
import com.lumos.domain.model.MaterialStock
import com.lumos.domain.model.OrderMaterial
import com.lumos.domain.model.OrderMaterialItem
import com.lumos.domain.model.OrderWithItems
import com.lumos.domain.model.Stockist
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterials(materials: List<MaterialStock>)

    @Query(
        """
        select * from material_stock 
        order by stockAvailable, materialName
    """
    )
    fun getMaterialsFlow(): Flow<List<MaterialStock>>

    @Query(
        """
        select * 
        from material_stock 
        where truckStockControl = 1
        order by stockAvailable, materialName
    """
    )
    fun getMaterialsTruckStockControlFlow(): Flow<List<MaterialStock>>

    @Query(
        """
        select distinct 
            parentMaterialId as materialId,
            parentMaterialId as materialStockId,
            '' as materialName,
            0 as stockQuantity,
            0 as stockAvailable,
            requestUnit,
            '' as type,
            0 as truckStockControl,
            parentMaterialId,
            materialBaseName,
            '' as materialPower,
            '' as materialBrand
        from material_stock
        order by materialBaseName
    """
    )
    fun getMaterialsOrderFlow(): Flow<List<MaterialStock>>

    @Query(
        """
        select count(*) from material_stock
    """
    )
    suspend fun materialCount(): Int

    @Query(
        """
        select * from deposit order by depositName
    """
    )
    fun getDepositsFlow(): Flow<List<Deposit>>

    @Query(
        """
        select * from stockist order by stockistName
    """
    )
    fun getStockistsFlow(): Flow<List<Stockist>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStockists(stockists: List<Stockist>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDeposits(deposits: List<Deposit>)

    @Query("delete from stockist")
    suspend fun deleteStockists()

    @Query("delete from deposit")
    suspend fun deleteDeposits()

    @Insert
    suspend fun insertOrder(order: OrderMaterial)

    @Insert
    suspend fun insertOrderItems(orderItems: List<OrderMaterialItem>)

    @Transaction
    @Query("SELECT * FROM order_material WHERE orderId = :orderId")
    suspend fun getOrderWithItems(orderId: String): OrderWithItems

    @Query(
        """
        UPDATE material_stock
        SET stockQuantity = CAST(stockQuantity AS NUMERIC) - CAST(:quantityExecuted AS NUMERIC),
            stockAvailable = CAST(stockAvailable AS NUMERIC) - CAST(:quantityExecuted AS NUMERIC)
        WHERE materialStockId = :materialStockId AND truckStockControl = 1
    """
    )
    suspend fun debitStock(
        materialStockId: Long,
        quantityExecuted: String
    )

    @Query("DELETE FROM material_stock")
    suspend fun deleteStock()


}
