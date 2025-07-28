package com.lumos.lumosspring.util

import com.lumos.lumosspring.authentication.repository.RefreshTokenRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.Normalizer
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*


@Component
class Util(
    private val jwtDecoder: JwtDecoder,
    private val jdbcTemplate: JdbcTemplate,
    private val refreshTokenRepository: RefreshTokenRepository
) {
    fun convertToBigDecimal(value: String?): BigDecimal? {
        if (value == null) {
            return null
        }
        try {
            val nf = NumberFormat.getInstance(Locale.of("pt", "BR"))
            val number = nf.parse(value)

            // Converter para BigDecimal
            return BigDecimal(number.toString())
        } catch (e: Exception) {
            return null
        }
    }

    fun getUserFromRToken(rToken: String?): UUID? {
        val tokenFromDb = refreshTokenRepository.findByToken(rToken)
        if (tokenFromDb.isEmpty) {
            return null
        }
        val jwt = jwtDecoder.decode(rToken)

        return tokenFromDb.get().user
    }

    // Método auxiliar para verificar se uma string está vazia ou é nula
    fun isEmpty(value: String?): Boolean {
        return value == null || value.trim { it <= ' ' }.isEmpty()
    }

    fun formatPrice(price: BigDecimal?): String {
        if (price != null) {
            // Formatação de número para garantir que a vírgula seja usada como separador decimal
            val df = DecimalFormat("#,##0.00")
            return df.format(price).replace('.', ',') // Troca o ponto por vírgula
        }
        return "0,00"
    }

    fun normalizeWord(word: String): String {
        return Normalizer.normalize(word, Normalizer.Form.NFD)
            .replace("\\p{M}".toRegex(), "").uppercase(Locale.getDefault()) // Remove caracteres diacríticos (acentos)
    }

    fun normalizeDate(date: Instant): String {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("America/Sao_Paulo"))

        return formatter.format(date)
    }

    fun extractNumber(value: String?): Double {
        return value?.filter { it.isDigit() || it == '.' }?.toDoubleOrNull() ?: 0.0
    }

    fun extractMaskToList(mask: String): List<Long> {
        if (mask.isBlank()) return emptyList()

        return mask.split("#")
            .filter { it.isNotBlank() }
            .map { it.toLong() }
    }


    private val allowedColumnsByTable = mapOf(
        "app_user" to setOf("user_id", "name", "last_name", "email", "phone_number"),
        "team" to setOf("id_team", "team_name", "team_phone", "driver_id", "electrician_id"),
        "material_reservation" to setOf("pre_measurement_street_id", "status"),
        "pre_measurement_street" to setOf("pre_measurement_street_id", "street_status"),
    )

    val columnTypesByTable = mapOf(
        "app_user" to mapOf(
            "user_id" to "uuid"
        ),
        "team" to mapOf(
            "driver_id" to "uuid",
            "electrician_id" to "uuid",
        )
    )

    fun getObject(request: UtilController.GetObjectRequest): List<Map<String, Any>> {
        val allowedColumns = allowedColumnsByTable[request.table.lowercase()]
            ?: throw IllegalArgumentException("Tabela não permitida: ${request.table}")

        request.fields.forEach { field ->
            if (!allowedColumns.contains(field)) {
                throw IllegalArgumentException("Campo $field não permitido para a tabela ${request.table}")
            }
        }

        if (!allowedColumns.contains(request.where)) {
            throw IllegalArgumentException("Campo ${request.where} não permitido para a tabela ${request.table}")
        }

        val placeholders = request.equal.joinToString(",") { "?" }
        val whereClause = "${request.where} IN ($placeholders)"
        val sql = "SELECT ${request.fields.joinToString()} FROM ${request.table} WHERE $whereClause"

        val columnType = columnTypesByTable[request.table.lowercase()]?.get(request.where)

        return if (columnType?.equals("uuid", ignoreCase = true) == true) {
            val uuidParams = request.equal.map { UUID.fromString(it.toString()) }.toTypedArray()
            jdbcTemplate.queryForList(sql, *uuidParams)
        } else {
            jdbcTemplate.queryForList(sql, *request.equal.toTypedArray())
        }
    }

    fun updateEntity(request: UtilController.UpdateEntity) {
        val allowedColumns = allowedColumnsByTable[request.table.lowercase()]
            ?: throw IllegalArgumentException("Tabela não permitida: ${request.table}")

        if (!allowedColumns.contains(request.field) || !allowedColumns.contains(request.where)) {
            throw IllegalArgumentException("Campo ${request.field} não permitido para a tabela ${request.table}")
        }


        val safeNameRegex = Regex("^[a-zA-Z0-9_]+\$")

        if (!safeNameRegex.matches(request.table) ||
            !safeNameRegex.matches(request.field) ||
            !safeNameRegex.matches(request.where)) {
            throw IllegalArgumentException("Identificadores inválidos")
        }


        // Validação do nome da tabela e campos — já feita acima.
        // Agora construímos a query de forma segura, parametrizada:
        val sql = "UPDATE ${request.table} SET ${request.field} = ? WHERE ${request.where} = ?"

        jdbcTemplate.update(sql, request.set, request.equal)
    }



}

object ContractStatus {
    const val ACTIVE = "ACTIVE"
    const val INACTIVE = "INACTIVE"
    const val ARCHIVED = "ARCHIVED"
}

object ContractType {
    const val INSTALLATION = "INSTALLATION"
    const val MAINTENANCE = "MAINTENANCE"
}

object ExecutionStatus {
    const val PENDING = "PENDING"
    const val VALIDATING = "VALIDATING"
    const val WAITING_CONTRACTOR = "WAITING_CONTRACTOR"
    const val AVAILABLE = "AVAILABLE"
    const val WAITING_STOCKIST = "WAITING_STOCKIST"
    const val WAITING_RESERVE_CONFIRMATION = "WAITING_RESERVE_CONFIRMATION"
    const val WAITING_COLLECT = "WAITING_COLLECT"
    const val AVAILABLE_EXECUTION = "AVAILABLE_EXECUTION"
    const val IN_PROGRESS = "IN_PROGRESS"
    const val FINISHED = "FINISHED"
}

object ItemStatus {
    const val PENDING = "PENDING"
    const val CANCELLED = "CANCELLED"
    const val APPROVED = "APPROVED"
}

object NotificationType {
    const val CONTRACT = "CONTRACT"
    const val UPDATE = "UPDATE"
    const val EVENT = "EVENT"
    const val WARNING = "WARNING"
    const val CASH = "CASH"
    const val ALERT = "ALERT"
}

object ReservationStatus {
    const val PENDING = "PENDING"
    const val APPROVED = "APPROVED"

    const val REJECTED = "REJECTED"
    const val CANCELLED = "CANCELLED"

    const val IN_STOCK = "IN_STOCK"
    const val COLLECTED = "COLLECTED"

    const val FINISHED = "FINISHED"
}