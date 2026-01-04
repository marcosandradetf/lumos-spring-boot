package com.lumos.lumosspring.contract.repository

import com.lumos.lumosspring.contract.entities.ItemRuleDistribution
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface ItemRuleDistributionRepository: CrudRepository<ItemRuleDistribution, Long> {
    @Query("""
        SELECT description
        FROM item_rule_distribution
        WHERE tenant_id = :currentTenantId
    """)
    fun findAllByTenantId(currentTenantId: UUID): List<String>
}