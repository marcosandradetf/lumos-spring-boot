package com.lumos.lumosspring.team.model

import com.lumos.lumosspring.authentication.model.TenantEntity
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table("stockist")
data class Stockist(
    @Id
    val stockistId: Long? = null,
    @Column("deposit_id_deposit")
    val depositId: Long,
    @Column("user_id_user")
    val userId: UUID,

    val notificationCode: UUID = UUID.randomUUID()
) : TenantEntity()