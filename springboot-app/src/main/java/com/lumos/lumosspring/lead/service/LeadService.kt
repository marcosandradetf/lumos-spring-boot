package com.lumos.lumosspring.lead.service

import com.lumos.lumosspring.authentication.model.Tenant
import com.lumos.lumosspring.authentication.repository.TenantRepository
import com.lumos.lumosspring.authentication.service.TokenService
import com.lumos.lumosspring.billing.service.SubscriptionLifecycleService
import com.lumos.lumosspring.lead.model.LeadRequest
import com.lumos.lumosspring.lead.repository.LeadRequestRepository
import com.lumos.lumosspring.notifications.service.EmailService
import com.lumos.lumosspring.scheduler.AsyncTenantContext
import com.lumos.lumosspring.user.model.AppUser
import com.lumos.lumosspring.user.model.Role
import com.lumos.lumosspring.user.model.UserStatus
import com.lumos.lumosspring.user.repository.UserRepository
import com.lumos.lumosspring.util.ErrorResponse
import com.lumos.lumosspring.util.Utils
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

@Service
class LeadService(
    private val leadRequestRepository: LeadRequestRepository,
    private val emailService: EmailService,
    private val tenantRepository: TenantRepository,
    private val userRepository: UserRepository,
    private val passwordEncoder: BCryptPasswordEncoder,
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    private val subscriptionLifecycleService: SubscriptionLifecycleService,
    private val tokenService: TokenService,
) {

    companion object {
        private const val MIN_PASSWORD_LENGTH = 8
    }

    /**
     * Cria tenant, usuário admin, subscription (trial ou plano pago ACTIVE) e devolve o mesmo payload do login (access + cookie refresh).
     *
     * @param operationFocus catálogo em [com.lumos.lumosspring.plan.controller.PublicPlanCatalogController] (ex.: Essencial, Profissional, Enterprise)
     * @param useTrial se true, [SubscriptionStatus.TRIAL] com 14 dias; se false, [SubscriptionStatus.ACTIVE] sem trial (checkout real virá depois)
     */
    @Transactional
    fun startFreeTest(
        useTrial: Boolean = true,
        firstName: String,
        lastName: String,
        phone: String = "",
        email: String,
        company: String = "",
        cnpj: String,
        teamSize: String = "",
        operationFocus: String,
        currentMoment: String,
        message: String? = null,
        password: String,
        response: HttpServletResponse,
        isMobile: Boolean = false,
    ): ResponseEntity<*> {
        val emailNorm = email.trim().lowercase()
        val cnpjNorm = cnpj.replace("\\D".toRegex(), "")
        if (emailNorm.isBlank()) {
            return ResponseEntity.badRequest().body(ErrorResponse("E-mail é obrigatório."))
        }
        if (password.length < MIN_PASSWORD_LENGTH) {
            return ResponseEntity.badRequest()
                .body(ErrorResponse("Senha deve ter pelo menos $MIN_PASSWORD_LENGTH caracteres."))
        }

        val companyArray = company.split(" ")
        if (companyArray.isEmpty()) {
            return ResponseEntity.badRequest().body(ErrorResponse("Empresa é obrigatório."))
        }
        val usernameNorm = "admin-${companyArray[0].trim().lowercase()}"

        if (!Utils.isValidCNPJ(cnpj)) {
            return ResponseEntity.badRequest().body(ErrorResponse("CNPJ inválido."))
        }

        if (userRepository.findByUsernameOrCpfCnpjIgnoreCase(
                usernameNorm,
                cnpjNorm
            ).isPresent
        ) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse("Este CNPJ ou esse Username já está cadastrado. Faça login ou recupere a senha."))
        }

        val newTenantId = UUID.randomUUID()
        val tenant = Tenant().apply {
            this.tenantId = newTenantId
            this.description = listOfNotNull(company.ifBlank { null }, "Trial").joinToString(" — ").ifBlank { "Lumos Trial" }
            this.bucket = "lumos"
            this.isNewEntry = true
        }
        tenantRepository.save(tenant)
        AsyncTenantContext.setTenant(newTenantId)

        val newUserId = UUID.randomUUID()
        val user = AppUser().apply {
            this.userId = newUserId
            isNewEntry = true
            username = usernameNorm
            this.password = passwordEncoder.encode(password)
            name = firstName.trim()
            this.lastName = lastName.trim()
            this.email = emailNorm
            this.cpfCnpj = cnpjNorm
            status = UserStatus.ACTIVE
            mustChangePassword = false
            support = false
            tenantId = newTenantId
            createdAt = OffsetDateTime.now()
            phoneNumber = phone.filter { it.isDigit() }
            mustChangePassword = false
            activationAttemptCount = 0
        }
        userRepository.save(user)

        insertUserRole(newUserId, Role.Values.ADMIN.roleId)

        val plan = operationFocus.trim().ifBlank { "Profissional" }
        if (useTrial) {
            subscriptionLifecycleService.createTrialSubscription(newTenantId, plan)
        } else {
            subscriptionLifecycleService.createActiveSubscriptionWithoutTrial(newTenantId, plan)
        }

        saveLeadAndNotifyComercial(
            firstName = firstName,
            lastName = lastName,
            email = emailNorm,
            phone = phone,
            company = company,
            teamSize = teamSize,
            message = message,
            cnpj = cnpj,
            operationFocus = operationFocus,
            currentMoment = currentMoment,
            useTrial = useTrial,
        )

        return tokenService.issueTokensForNewUser(newUserId, response, isMobile)
    }

    private fun insertUserRole(userId: UUID, roleId: Long) {
        val params = MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("roleId", roleId)
        namedParameterJdbcTemplate.update(
            """
            INSERT INTO user_role (id_user, id_role)
            VALUES (:userId, :roleId)
            """.trimIndent(),
            params,
        )
    }

    private fun saveLeadAndNotifyComercial(
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        company: String,
        teamSize: String,
        message: String?,
        cnpj: String,
        operationFocus: String,
        currentMoment: String,
        useTrial: Boolean
    ) {
        val leadRequest = LeadRequest().apply {
            type = "FREE_TEST_SIGNUP"
            this.firstName = firstName
            this.lastName = lastName
            this.email = email
            this.phone = phone.ifBlank { "—" }
            this.company = company.ifBlank { "—" }
            this.teamSize = teamSize.ifBlank { "—" }
            this.message = message ?: "Cadastro self-service trial"
            priority = 10
            status = "RECEIVED"
        }
        leadRequestRepository.save(leadRequest)

        try {
            val subject = "🚀 Novo Teste Grátis: ${company.ifBlank { email }}"

            // Filtra apenas números para o link do WhatsApp
            val cleanPhone = phone.filter { it.isDigit() }

            // Mensagens pré-definidas
            val waText = "Olá, $firstName! Sou do Lumos IP. Vi que você iniciou seu primeiro acesso para a $company. Parabéns por modernizar sua operação! 🚀 Gostaria de agendar um treinamento rápido de 15 min para configurarmos seus primeiros contratos juntos?"
            val emailMsg = "Olá, $firstName, tudo bem?\n\nNotei que você ativou o teste gratuito do Lumos IP para a $company. Nosso objetivo é garantir que você tenha visibilidade total da sua operação de campo.\n\nEstou à disposição para um treinamento rápido. Qual o melhor horário para conversarmos?"

            // Encoders
            val encodedWa = java.net.URLEncoder.encode(waText, "UTF-8")
            val encodedEmailSubject = java.net.URLEncoder.encode("Boas-vindas ao Lumos IP - $company", "UTF-8")
            val encodedEmailBody = java.net.URLEncoder.encode(emailMsg, "UTF-8")

            val waLink = "https://wa.me/$cleanPhone?text=$encodedWa"
            val mailtoLink = "mailto:$email?subject=$encodedEmailSubject&body=$encodedEmailBody"

            val emailBody = """
                <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; color: #1f2937; line-height: 1.5;">
                    <div style="background-color: #10b981; padding: 20px; border-radius: 12px 12px 0 0; text-align: center;">
                        <h2 style="color: white; margin: 0;">🚀 Nova Conta Criada</h2>
                        <p style="color: #ecfdf5; margin: 5px 0 0 0;">O trial para a <strong>${company.ifBlank { "Empresa não informada" }}</strong> já está ativo.</p>
                    </div>
                    
                    <div style="background: #ffffff; padding: 25px; border: 1px solid #e5e7eb; border-top: none; border-radius: 0 0 12px 12px; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);">
                        
                        <h4 style="color: #6b7280; text-transform: uppercase; letter-spacing: 0.05em; font-size: 12px; margin-bottom: 15px;">Ações de Conversão</h4>
                        <div style="margin-bottom: 30px; display: flex; gap: 10px;">
                            <a href="$waLink" style="background-color: #25d366; color: white; padding: 12px 20px; text-decoration: none; border-radius: 8px; font-weight: bold; display: inline-block; font-size: 14px;">
                                🟢 WhatsApp
                            </a>
                            &nbsp;
                            <a href="$mailtoLink" style="background-color: #3b82f6; color: white; padding: 12px 20px; text-decoration: none; border-radius: 8px; font-weight: bold; display: inline-block; font-size: 14px;">
                                🔵 Enviar E-mail
                            </a>
                        </div>
            
                        <hr style="border: 0; border-top: 1px solid #f3f4f6; margin-bottom: 25px;">
            
                        <table style="width: 100%; border-collapse: collapse;">
                            <tr>
                                <td style="padding-bottom: 20px;">
                                    <h4 style="color: #6b7280; text-transform: uppercase; font-size: 12px; margin: 0 0 5px 0;">Responsável</h4>
                                    <p style="margin: 0; font-weight: 600;">$firstName $lastName</p>
                                    <p style="margin: 2px 0 0 0; font-size: 14px; color: #4b5563;">$email</p>
                                </td>
                                <td style="padding-bottom: 20px;">
                                    <h4 style="color: #6b7280; text-transform: uppercase; font-size: 12px; margin: 0 0 5px 0;">CNPJ</h4>
                                    <p style="margin: 0; font-weight: 600;">${cnpj.ifBlank { "—" }}</p>
                                </td>
                            </tr>
                            <tr>
                                <td style="padding-bottom: 20px;">
                                    <h4 style="color: #6b7280; text-transform: uppercase; font-size: 12px; margin: 0 0 5px 0;">Operação</h4>
                                    <p style="margin: 0; font-weight: 600;">Plano: ${operationFocus.ifBlank { "—" }}</p>
                                </td>
                                <td style="padding-bottom: 20px;">
                                    <h4 style="color: #6b7280; text-transform: uppercase; font-size: 12px; margin: 0 0 5px 0;">Equipe</h4>
                                    <p style="margin: 0; font-weight: 600;">${teamSize.ifBlank { "—" }} colab.</p>
                                </td>
                            </tr>
                        </table>
            
                        <div style="background-color: #f9fafb; padding: 15px; border-radius: 8px; border-left: 4px solid #10b981;">
                            <h4 style="margin: 0 0 5px 0; font-size: 12px; color: #6b7280; text-transform: uppercase;">Momento da Empresa</h4>
                            <p style="margin: 0; font-size: 14px;">$currentMoment</p>
                        </div>
            
                        <div style="margin-top: 20px;">
                            <h4 style="margin: 0 0 5px 0; font-size: 12px; color: #6b7280; text-transform: uppercase;">Mensagem Adicional</h4>
                            <p style="margin: 0; font-size: 14px; color: #4b5563; font-style: italic;">"${(message ?: "Nenhuma mensagem enviada.").ifBlank { "Cadastro self-service." }}"</p>
                        </div>
                    </div>
                    
                    <p style="text-align: center; font-size: 12px; color: #9ca3af; margin-top: 20px;">
                        Este é um e-mail automático gerado pelo sistema <strong>Lumos IP</strong>.
                    </p>
                </div>
            """.trimIndent()

            emailService.sendEmail("comercial@lumosip.com.br", subject, emailBody)
        } catch (e: Exception) {
            println("Erro ao processar e-mail comercial: ${e.message}")
        }
    }

    fun demoOrTestRequest(
        type: String,
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        company: String,
        teamSize: String,
        message: String?,
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
                    <p><strong>WhatsApp:</strong> <a href="https://wa.me/${
                phone.replace(
                    Regex("[^0-9]"),
                    ""
                )
            }">$phone</a></p>
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
