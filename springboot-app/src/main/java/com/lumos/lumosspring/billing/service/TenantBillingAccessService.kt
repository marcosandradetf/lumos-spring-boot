package com.lumos.lumosspring.billing.service

import com.lumos.lumosspring.billing.domain.SubscriptionStatus
import com.lumos.lumosspring.billing.repository.SubscriptionRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * Regra única de acesso ao app conforme [SubscriptionStatus] e trial.
 *
 * @param strictSubscription se true, tenant **sem** linha em `subscription` não acessa (SaaS novo).
 * Se false (legado / migração), ausência de subscription permite acesso.
 */
@Service
class TenantBillingAccessService(
    private val subscriptionRepository: SubscriptionRepository,
    @Value("\${lumos.billing.strict-subscription:false}") private val strictSubscription: Boolean,
) {
    fun isTenantAccessAllowed(tenantId: UUID): Pair<Boolean, String?> {
        val sub = subscriptionRepository.findByTenantId(tenantId).orElse(null) ?: return Pair(!strictSubscription, "")
        val status = SubscriptionStatus.fromCodeOrNull(sub.status) ?: return Pair(false, "")
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        return Pair(status.allowsAppAccess(now, sub.trialEndsAt), sub.status)
    }
}
