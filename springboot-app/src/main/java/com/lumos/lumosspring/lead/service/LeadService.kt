package com.lumos.lumosspring.lead.service

import com.lumos.lumosspring.authentication.model.Tenant
import com.lumos.lumosspring.authentication.repository.TenantRepository
import com.lumos.lumosspring.authentication.service.TokenService
import com.lumos.lumosspring.billing.service.SubscriptionLifecycleService
import com.lumos.lumosspring.lead.model.LeadRequest
import com.lumos.lumosspring.lead.repository.LeadRequestRepository
import com.lumos.lumosspring.notifications.service.EmailService
import com.lumos.lumosspring.user.model.AppUser
import com.lumos.lumosspring.user.model.Role
import com.lumos.lumosspring.user.repository.UserRepository
import com.lumos.lumosspring.util.ErrorResponse
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
     * @param planName catálogo em [com.lumos.lumosspring.plan.controller.PublicPlanCatalogController] (ex.: Essencial, Profissional, Enterprise)
     * @param useTrial se true, [SubscriptionStatus.TRIAL] com 14 dias; se false, [SubscriptionStatus.ACTIVE] sem trial (checkout real virá depois)
     */
    @Transactional
    fun startFreeTest(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        response: HttpServletResponse,
        phone: String = "",
        company: String = "",
        teamSize: String = "",
        message: String? = null,
        planName: String = "Profissional",
        useTrial: Boolean = true,
        isMobile: Boolean = false,
    ): ResponseEntity<*> {
        val emailNorm = email.trim().lowercase()
        if (emailNorm.isBlank()) {
            return ResponseEntity.badRequest().body(ErrorResponse("E-mail é obrigatório."))
        }
        if (password.length < MIN_PASSWORD_LENGTH) {
            return ResponseEntity.badRequest().body(ErrorResponse("Senha deve ter pelo menos $MIN_PASSWORD_LENGTH caracteres."))
        }
        if (userRepository.findByUsernameIgnoreCase(emailNorm).isPresent) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse("Este e-mail já está cadastrado. Faça login ou recupere a senha."))
        }

        val newTenantId = UUID.randomUUID()
        val tenant = Tenant().apply {
            setTenantId(newTenantId)
            setDescription(
                listOfNotNull(company.ifBlank { null }, "Trial").joinToString(" — ").ifBlank { "Lumos Trial" },
            )
            setBucket("tenant-${newTenantId.toString().replace("-", "")}")
        }
        tenantRepository.save(tenant)

        val cpf = generateUniqueCpf()
        val newUserId = UUID.randomUUID()
        val now = OffsetDateTime.now()
        val user = AppUser().apply {
            setUserId(newUserId)
            isNewEntry = true
            username = emailNorm
            this.password = passwordEncoder.encode(password)
            name = firstName.trim()
            this.lastName = lastName.trim()
            this.email = emailNorm
            this.cpf = cpf
            status = true
            support = false
            tenantId = newTenantId
            createdAt = now
        }
        val saved = userRepository.save(user)

        insertUserRole(saved.getUserId(), Role.Values.ADMIN.roleId)

        val plan = planName.trim().ifBlank { "Profissional" }
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
        )

        return tokenService.issueTokensForNewUser(saved.getUserId(), response, isMobile)
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

    /**
     * CPF sintético válido e único (login usa e-mail como username; CPF não é exibido no trial).
     */
    private fun generateUniqueCpf(): String {
        repeat(80) {
            val cpf = generateRandomValidCpf()
            if (userRepository.findByCpfIgnoreCase(cpf).isEmpty) {
                return cpf
            }
        }
        throw IllegalStateException("Não foi possível gerar CPF único para o cadastro")
    }

    private fun generateRandomValidCpf(): String {
        val r = ThreadLocalRandom.current()
        while (true) {
            val d = IntArray(9) { r.nextInt(10) }
            if (d.all { it == d[0] }) continue
            var s1 = 0
            for (i in 0 until 9) s1 += d[i] * (10 - i)
            val dv1 = if (s1 % 11 < 2) 0 else 11 - (s1 % 11)
            var s2 = 0
            for (i in 0 until 9) s2 += d[i] * (11 - i)
            s2 += dv1 * 2
            val dv2 = if (s2 % 11 < 2) 0 else 11 - (s2 % 11)
            return d.joinToString("") + dv1 + dv2
        }
    }

    private fun saveLeadAndNotifyComercial(
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        company: String,
        teamSize: String,
        message: String?,
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
            val subject = "🚀 Novo Teste Grátis (conta criada): ${company.ifBlank { email }}"
            val emailBody = """
            <div style="font-family: sans-serif; max-width: 600px; color: #18181b;">
                <h2 style="color: #10b981;">Conta trial criada</h2>
                <p>Um usuário acabou de criar conta e já recebeu acesso ao sistema.</p>
                <div style="background: #f4f4f5; padding: 20px; border-radius: 12px; border: 1px solid #e4e4e7;">
                    <p><strong>Nome:</strong> $firstName $lastName</p>
                    <p><strong>E-mail (login):</strong> <a href="mailto:$email">$email</a></p>
                    <p><strong>Empresa:</strong> ${company.ifBlank { "—" }}</p>
                    <p><strong>WhatsApp:</strong> ${phone.ifBlank { "—" }}</p>
                    <p><strong>Time:</strong> ${teamSize.ifBlank { "—" }}</p>
                </div>
            </div>
            """.trimIndent()
            emailService.sendEmail("comercial@lumosip.com.br", subject, emailBody)
        } catch (e: Exception) {
            println("Erro ao enviar e-mail comercial (trial cadastrado): ${e.message}")
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
