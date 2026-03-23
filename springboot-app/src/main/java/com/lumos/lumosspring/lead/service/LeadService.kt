package com.lumos.lumosspring.lead.service

import com.lumos.lumosspring.lead.model.LeadRequest
import com.lumos.lumosspring.lead.repository.LeadRequestRepository
import com.lumos.lumosspring.notifications.service.EmailService
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class LeadService(
    private val leadRequestRepository: LeadRequestRepository,
    private val emailService: EmailService
) {

    fun demoOrTestRequest(
        type: String,
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        company: String,
        teamSize: String,
        message: String?
    ): ResponseEntity<Any> {

        // 1. Criar e Salvar o Lead no Postgres (Prioridade Máxima)
        val leadRequest = LeadRequest().apply {
            this.type = type
            this.firstName = firstName
            this.lastName = lastName
            this.email = email
            this.phone = phone
            this.company = company
            this.teamSize = teamSize
            this.message = message ?: "Sem mensagem adicional"
            this.priority = if (type == "DEMO") 5 else 10
            this.status = "RECEIVED"
        }

        val savedLead = leadRequestRepository.save(leadRequest)

        // 2. Tentar enviar o e-mail (Com tratamento de erro para o limite do Resend)
        try {
            val subject = if (type == "DEMO") "⚡ Nova Demonstração: $company" else "🚀 Novo Teste Grátis: $company"

            val emailBody = """
            <div style="font-family: sans-serif; max-width: 600px; color: #18181b;">
                <h2 style="color: #10b981;">${if (type == "DEMO") "Solicitação de Demo" else "Novo Teste Grátis"}</h2>
                <p>Um novo lead acaba de chegar pelo site do <strong>Lumos</strong>.</p>
                
                <div style="background: #f4f4f5; padding: 20px; border-radius: 12px; border: 1px solid #e4e4e7;">
                    <p><strong>Cliente:</strong> $firstName $lastName</p>
                    <p><strong>Empresa:</strong> $company</p>
                    <p><strong>E-mail:</strong> <a href="mailto:$email">$email</a></p>
                    <p><strong>WhatsApp:</strong> <a href="https://wa.me/${phone.replace(Regex("[^0-9]"), "")}">$phone</a></p>
                    <p><strong>Tamanho do Time:</strong> $teamSize</p>
                    <hr style="border: 0; border-top: 1px solid #d4d4d8; margin: 15px 0;">
                    <p><strong>Mensagem do cliente:</strong><br>${message ?: "Nenhuma"}</p>
                </div>
                
                <p style="font-size: 12px; color: #71717a; margin-top: 20px;">
                    ID do Lead no Banco: ${savedLead.id} | Prioridade: ${leadRequest.priority} (0-10)
                </p>
            </div>
        """.trimIndent()

            emailService.sendEmail("comercial@lumosip.com.br", subject, emailBody)

        } catch (e: Exception) {
            // Se o Resend der 429 (limite), o lead já está salvo no banco.
            // Você pode logar aqui ou disparar um FCM de erro técnico.
            println("Erro ao enviar e-mail comercial, mas lead foi salvo: ${e.message}")
        }

        // 3. Notificação FCM para o SEU celular (O "Pulo do Gato")
        // fcmService.sendToAdmin("Novo Lead $type", "$company solicitou acesso.")

        return ResponseEntity.ok(mapOf("success" to true, "message" to "Solicitação processada com sucesso"))
    }
}