package com.lumos.lumosspring.billing.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Table("subscription")
class Subscription {
    @Id
    var subscriptionId: UUID? = null
    var tenantId: UUID? = null
    var planName: String? = null
    var status: String? = null
    var billingCycle: String? = null
    var trialEndsAt: OffsetDateTime? = null
    var currentPeriodStart: LocalDate? = null
    var currentPeriodEnd: LocalDate? = null
    var pricePerUserMonthlySnapshot: BigDecimal? = null
    var pricePerUserYearlySnapshot: BigDecimal? = null
    var currency: String? = "BRL"
    var paymentProviderCustomerId: String? = null
    var paymentProvider: String? = null
    var cancelAtPeriodEnd: Boolean? = false
    var canceledAt: OffsetDateTime? = null
    var createdAt: OffsetDateTime? = null
    var updatedAt: OffsetDateTime? = null
}
