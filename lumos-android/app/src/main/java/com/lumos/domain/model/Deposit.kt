package com.lumos.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deposits")
data class Deposit(
    @PrimaryKey val depositId: Long,
    val depositName: String,
    val regionName: String,
    val companyName: String,
)