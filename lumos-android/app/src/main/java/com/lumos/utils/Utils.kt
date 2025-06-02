package com.lumos.utils

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

object Utils {
    val dateTime: Instant
        get() {
            val date = ZonedDateTime.now(
                ZoneId.of("America/Sao_Paulo")
            )

            return date.toInstant()
        }

    fun timeSinceCreation(createdAt: Instant): String {
        val duration = Duration.between(createdAt, dateTime)

        return when {
            duration.toDays() > 0 -> "${duration.toDays()}d"
            duration.toHours() > 0 -> "${duration.toHours()}h"
            else -> "${duration.toMinutes()}min"
        }
    }

    fun formatPhoneNumber(number: String): String {
        val digits = number.filter { it.isDigit() }

        return when {
            digits.length >= 11 -> { // (XX) XXXXX-XXXX
                "(${digits.substring(0, 2)}) ${digits.substring(2, 7)}-${digits.substring(7, 11)}"
            }
            digits.length >= 10 -> { // (XX) XXXX-XXXX
                "(${digits.substring(0, 2)}) ${digits.substring(2, 6)}-${digits.substring(6, 10)}"
            }
            else -> digits // sem formatação, se muito curto
        }
    }

    fun formatDouble(value: Double): String {
        return value.toBigDecimal().stripTrailingZeros().toPlainString()
    }

}