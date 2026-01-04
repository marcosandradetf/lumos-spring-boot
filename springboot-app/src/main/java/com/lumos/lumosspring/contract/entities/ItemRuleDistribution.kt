package com.lumos.lumosspring.contract.entities

import com.lumos.lumosspring.authentication.model.TenantEntity
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table
data class ItemRuleDistribution(
    @Id
    val itemRuleDistributionId: Long? = null,
    val type: String,
    val itemDependency: String? = null,
) : TenantEntity()
