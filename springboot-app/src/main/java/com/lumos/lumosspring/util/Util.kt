package com.lumos.lumosspring.util

import com.lumos.lumosspring.authentication.repository.RefreshTokenRepository
import com.lumos.lumosspring.user.User
import org.springframework.dao.EmptyResultDataAccessException
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

    fun getUserFromRToken(rToken: String?): User? {
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

    val dateTime: Instant
        get() {
            val date = ZonedDateTime.now(
                ZoneId.of("America/Sao_Paulo")
            )

            return date.toInstant()
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

    fun timeSinceCreation(createdAt: Instant): String {
        val duration = Duration.between(createdAt, dateTime)

        return when {
            duration.toDays() > 0 -> "${duration.toDays()}d"
            duration.toHours() > 0 -> "${duration.toHours()}h"
            else -> "${duration.toMinutes()}min"
        }
    }

    fun extractMaskToList(mask: String): List<Long> {
        if (mask.isBlank()) return emptyList()

        return mask.split("#")
            .filter { it.isNotBlank() }
            .map { it.toLong() }
    }

    fun <T> getDescriptions(
        field: String,
        table: String,
        where: String,
        type: Class<T>,
        order: String = "",
    ): List<T> {
        var sql = "SELECT $field FROM $table WHERE $where"
        if (order != "") sql += " ORDER BY $order"

        return jdbcTemplate.queryForList(sql, type)
    }

    fun <T> getDescription(
        field: String,
        table: String,
        where: String = "",
        equal: String = "",
        type: Class<T>,
        order: String = ""
    ): T? {
        var sql = "SELECT $field FROM $table"
        if (where.isNotBlank() && equal.isNotBlank()) sql += " WHERE $where = $equal"
        if (order.isNotBlank()) sql += " ORDER BY $order"
        sql += " LIMIT 1"

        return try {
            jdbcTemplate.queryForObject(sql, type)
        } catch (ex: EmptyResultDataAccessException) {
            null // ou você pode lançar uma exceção customizada, se preferir
        }
    }

    private val allowedColumnsByTable = mapOf(
        "tb_users" to setOf("id_user", "name", "last_name", "email", "phone_number"),
        "tb_teams" to setOf("id_team", "team_name", "team_phone"),
    )

    val columnTypesByTable = mapOf(
        "tb_users" to mapOf(
            "id_user" to "uuid"
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


}

object ContractStatus {
    const val PENDING = "PENDING"
    const val VALIDATING = "VALIDATING"
    const val WAITING_CONTRACTOR = "WAITING_CONTRACTOR"
    const val AVAILABLE = "AVAILABLE"
    const val WAITING_STOCKIST = "WAITING_STOCKIST"
    const val WAITING_RESERVE_CONFIRMATION = "WAITING_RESERVE_CONFIRMATION"
    const val AVAILABLE_EXECUTION = "AVAILABLE_EXECUTION"
    const val IN_PROGRESS = "IN_PROGRESS"
    const val FINISHED = "FINISHED"
}

object ContractType {
    const val INSTALLATION = "INSTALLATION"
    const val MAINTENANCE = "MAINTENANCE"
}

object ItemStatus {
    const val PENDING = "PENDING"
    const val CANCELLED = "CANCELLED"
    const val APPROVED = "APPROVED"
    const val IN_PROGRESS = "IN_PROGRESS"
    const val FINISHED = "FINISHED"
}

object NotificationType {
    const val CONTRACT = "CONTRACT"
    const val UPDATE = "UPDATE"
    const val EVENT = "EVENT"
    const val WARNING = "ALERT"
    const val CASH = "CASH"
    const val ALERT = "CASH"
}

object ReservationStatus {
    const val PENDING = "PENDING"
    const val APPROVED = "APPROVED"
    const val COLLECTED = "COLLECTED"
    const val CANCELLED = "CANCELLED"
    const val FINISHED = "FINISHED"
}