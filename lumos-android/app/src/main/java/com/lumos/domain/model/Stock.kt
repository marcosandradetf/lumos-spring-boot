package com.lumos.domain.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.Instant

@Entity(
    tableName = "material_stock",
    primaryKeys = ["materialId", "materialStockId"]
)
data class MaterialStock(
    val materialId: Long,
    val materialStockId: Long,
    val materialName: String,
    val specs: String?,
    val stockQuantity: Double,
    val stockAvailable: Double,
    val requestUnit: String,
    val type: String,
)

@Entity(tableName = "stockist")
data class Stockist(
    @PrimaryKey val stockistId: Long,
    val stockistName: String,
    val stockistPhone: String?,
    val depositId: Long
)

@Entity(tableName = "deposit")
data class Deposit(
    @PrimaryKey val depositId: Long,
    val depositName: String,
    val depositAddress: String?,
    val depositPhone: String?,
)

data class StockResponse(
    val materialsStock: List<MaterialStock>,
    val deposits: List<Deposit>,
    val stockists: List<Stockist>,
)

@Entity(tableName = "order_material")
data class OrderMaterial(
    @PrimaryKey
    val orderId: String,
    val orderCode: String,
    val createdAt: String = Instant.now().toString(),
    val depositId: Long
)

@Entity(tableName = "order_material_item", primaryKeys = ["orderId", "materialId"])
data class OrderMaterialItem(
    val orderId: String,
    val materialId: Long,
)

data class OrderWithItems(
    @Embedded val orderMaterial: OrderMaterial,
    @Relation(
        parentColumn = "orderId",
        entityColumn = "orderId"
    )
    val items: List<OrderMaterialItem>
)
