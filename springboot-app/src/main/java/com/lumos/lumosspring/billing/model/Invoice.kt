package com.lumos.lumosspring.billing.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Table("invoice")
class Invoice {
    @Id
    var invoiceId: UUID? = null
    var subscriptionId: UUID? = null
    var tenantId: UUID? = null
    var periodStart: LocalDate? = null
    var periodEnd: LocalDate? = null
    var billingCycle: String? = null
    var seatCount: Int? = null
    var unitPriceSnapshot: BigDecimal? = null
    var amountTotal: BigDecimal? = null
    var currency: String? = "BRL"
    var status: String? = null
    var issuedAt: OffsetDateTime? = null
    var dueAt: OffsetDateTime? = null
    var paidAt: OffsetDateTime? = null
    var externalPaymentId: String? = null
}
