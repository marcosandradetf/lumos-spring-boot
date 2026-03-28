package com.lumos.lumosspring.billing.integration

import java.util.UUID

/**
 * Abstração para Stripe / Pagar.me / etc.
 * Implementação real registra customer, anexa método de pagamento e confirma invoice.
 */
interface PaymentGatewayPort {
    fun ensureCustomer(tenantId: UUID, billingEmail: String): String

    fun chargeInvoice(externalInvoiceId: String): Boolean
}

/**
 * Placeholder até integrar gateway (não chama rede).
 */
@org.springframework.context.annotation.Primary
@org.springframework.stereotype.Component
class NoOpPaymentGateway : PaymentGatewayPort {
    override fun ensureCustomer(tenantId: UUID, billingEmail: String): String = "noop_cust_$tenantId"

    override fun chargeInvoice(externalInvoiceId: String): Boolean = true
}
