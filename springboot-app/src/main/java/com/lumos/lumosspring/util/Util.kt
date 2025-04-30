package com.lumos.lumosspring.util

import com.lumos.lumosspring.authentication.repository.RefreshTokenRepository
import com.lumos.lumosspring.user.User
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
class Util(private val jwtDecoder: JwtDecoder, private val refreshTokenRepository: RefreshTokenRepository) {
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

}

object ContractStatus {
    const val PENDING = "PENDING"
    const val VALIDATING = "VALIDATING"
    const val WAITING_CONTRACTOR  = "WAITING_CONTRACTOR"
    const val AVAILABLE = "AVAILABLE"
    const val WAITING_TEAM = "WAITING_TEAM"
    const val WAITING_STOCKIST = "WAITING_TEAM"
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
    const val CONTRACT ="CONTRACT"
    const val UPDATE ="UPDATE"
    const val EVENT ="EVENT"
    const val WARNING ="ALERT"
    const val CASH ="CASH"
    const val ALERT ="CASH"
}

object ReservationStatus {
    const val PENDING = "PENDING"
    const val COLLECTED = "COLLECTED"
    const val CANCELLED = "CANCELLED"
}