package com.lumos.lumosspring.billing.repository

import com.lumos.lumosspring.billing.model.Invoice
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
interface InvoiceRepository : CrudRepository<Invoice, UUID> {
    fun existsBySubscriptionIdAndPeriodStart(subscriptionId: UUID, periodStart: LocalDate): Boolean
}
