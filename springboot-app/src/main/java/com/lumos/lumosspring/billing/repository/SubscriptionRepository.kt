package com.lumos.lumosspring.billing.repository

import com.lumos.lumosspring.billing.model.Subscription
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface SubscriptionRepository : CrudRepository<Subscription, UUID> {
    fun findByTenantId(tenantId: UUID): Optional<Subscription>
}
