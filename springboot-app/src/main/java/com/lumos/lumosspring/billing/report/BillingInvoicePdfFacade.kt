package com.lumos.lumosspring.billing.report

import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Ponto único para gerar PDF de fatura no futuro.
 *
 * Fluxo sugerido (alinhado ao restante do app):
 * 1. Montar HTML da fatura (Thymeleaf ou template string).
 * 2. Chamar [com.lumos.lumosspring.util.Utils.sendHtmlToPuppeteer] para PDF.
 * 3. Opcional: anexar ao e-mail de cobrança (Resend) ou salvar em S3/R2.
 *
 * Proration futura: incluir linhas com dias proporcionais no HTML antes do Puppeteer.
 */
@Component
class BillingInvoicePdfFacade {
    fun renderPlaceholder(invoiceId: UUID): ByteArray =
        "<html><body><h1>Fatura $invoiceId</h1><p>TODO: template + Utils.sendHtmlToPuppeteer</p></body></html>"
            .toByteArray(Charsets.UTF_8)
}
