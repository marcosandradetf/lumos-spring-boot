package com.lumos.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.text.toLowerCase
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import java.util.UUID

object Utils {
    val dateTime: Instant
        get() {
            val date = ZonedDateTime.now(
                ZoneId.of("America/Sao_Paulo")
            )

            return date.toInstant()
        }

    private fun parseTimestamptzToInstant(raw: String): Instant {
        // PostgreSQL retorna algo como "2025-07-27 22:34:10.952689+00"
        // Ajuste para ISO 8601: "2025-07-27T22:34:10.952689+00:00"
        val formatted = raw
            .replace(" ", "T")
            .replace(Regex("""\+(\d{2})$"""), "+$1:00")  // transforma +00 → +00:00

        return Instant.parse(formatted)
    }


    fun timeSinceCreation(createdAtRaw: String): String {
        val createdAt = parseTimestamptzToInstant(createdAtRaw)
        val zoneId = ZoneId.of("America/Sao_Paulo")

        val createdAtZoned = createdAt.atZone(zoneId)
        val nowZoned = ZonedDateTime.now(zoneId)

        val duration = Duration.between(createdAtZoned, nowZoned)

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
        val formatted = String.format(Locale.US, "%.1f", value)
        return if (formatted.endsWith(".0")) {
            formatted.dropLast(2)
        } else {
            formatted
        }
    }

    fun buildAddress(
        streetName: String?,
        number: String? = null,
        hood: String? = null,
        city: String? = null,
        state: String? = null
    ): String {
        val part1 = listOfNotNull(
            streetName?.takeIf { it.isNotBlank() },
            number?.takeIf { it.isNotBlank() }
        ).joinToString(", ")

        val part2 = hood?.takeIf { it.isNotBlank() }

        val part3 = listOfNotNull(
            city?.takeIf { it.isNotBlank() },
            state?.takeIf { it.isNotBlank() }
        ).joinToString(" - ")

        return listOf(part1, part2, part3)
            .filter { !it.isNullOrBlank() }
            .joinToString(", ")
    }

    fun parseToAny(text: String): Any {
        return when {
            text.equals("true", ignoreCase = true) -> true
            text.equals("false", ignoreCase = true) -> false
            text.toLongOrNull() != null -> text.toLong()
            text.toDoubleOrNull() != null -> text.toDouble()
            else -> text // volta como String se nada casar
        }
    }

    fun getFileFromUri(context: Context, uri: Uri, fileName: String): File {
        val inputStream = context.contentResolver.openInputStream(uri)!!
        val file = File(context.cacheDir, fileName)
        file.outputStream().use { inputStream.copyTo(it) }
        return file
    }


    fun compressImageFromUri(
        context: Context,
        imageUri: Uri,
        quality: Int = 70 // Qualidade de 0 a 100
    ): ByteArray? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

            outputStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun sanitizeDecimalInput(input: String): String {
        var replaced = input.replace(',', '.')

        // Adiciona 0 à esquerda se começar com ponto
        if (replaced.startsWith(".")) replaced = "0$replaced"

        // Remove caracteres inválidos (mantém só dígitos e ponto)
        replaced = replaced.filter { it.isDigit() || it == '.' }

        // Garante apenas um ponto: mantém só o primeiro ponto
        val firstDotIndex = replaced.indexOf('.')
        if (firstDotIndex != -1) {
            val before = replaced.substring(0, firstDotIndex).trimStart('0').ifEmpty { "0" }
            val afterRaw = replaced.substring(firstDotIndex + 1)

            // Remove pontos extras de afterRaw (caso o usuário tenha digitado mais de um ponto)
            val afterClean = afterRaw.filter { it.isDigit() }

            val after = afterClean.take(1) // Limita a 1 dígito
            return "$before.$after"
        }

        // Caso não tenha ponto, remove zeros à esquerda
        return replaced.trimStart('0').ifEmpty { "0" }
    }

    fun uuidToShortCodeWithPrefix(prefix: String, strUUID: String, length: Int = 10): String {
        val uuid = try {
            UUID.fromString(strUUID)
        } catch (e: Exception) {
            throw IllegalStateException(e.message)
        }

        val mostSigBits = uuid.mostSignificantBits
        val base36 = java.lang.Long.toUnsignedString(mostSigBits, 36).uppercase()

        // Ajusta tamanho fixo e adiciona prefixo REQ-
        val code = base36.padStart(length, '0').takeLast(length)
        return "$prefix-$code"
    }

    fun abbreviate(name: String): String {
        val tokens = name.split(" ")
        val result = mutableListOf<String>()

        var replace = false

        for (token in tokens) {
            val word = token.uppercase()

            when {
                word.lowercase() == "prefeitura" -> {
                    result.add("PREF.")
                    replace = true
                }
                word.lowercase() == "municipal" && replace -> {
                    result.add("MUN.")
                }
                word.lowercase() == "de" && replace -> {
                    // ignora "DE" após PREFEITURA ou MUNICIPAL
                }
                else -> {
                    result.add(token)
                    replace = false
                }
            }
        }

        return result.joinToString(" ") { word ->
            if (word.lowercase() == "de") {
                "de"
            } else {
                word.lowercase().replaceFirstChar { it.uppercase() }
            }
        }
    }

    fun hasFullName(input: String): Boolean {
        val parts = input.trim().split("\\s+".toRegex())
        return parts.size >= 2 && parts.all { it.length >= 2 }
    }

}