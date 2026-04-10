package com.lumos.lumosspring.stock.deposit.dto

data class StockistModel(
    val stockistId: Long?,
    val userId: String,
    val name: String,
    val depositId: Long,
    val depositName: String,
    val depositAddress: String,
    val depositPhone: String,
    val region: String,
)

data class DepositByStockist(
    val depositId: Long,
    val depositName: String,
    val depositAddress: String?,
    val depositPhone: String?,
)
