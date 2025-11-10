package com.lumos.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ContractItemBalance(
    @PrimaryKey
    val contractItemId: Long,
    val currentBalance: String,
)
