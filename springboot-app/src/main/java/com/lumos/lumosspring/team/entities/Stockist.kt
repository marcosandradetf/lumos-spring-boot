package com.lumos.lumosspring.team.entities

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table("stockist")
data class Stockist(
    @Id
    val stockistId: Long = 0,
    @Column("deposit_id_deposit")
    val depositId: Long,
    @Column("user_id_user")
    val userId: UUID
) {
    fun getStockistCode(): String {
        return "${stockistId}_$userId"
    }
}