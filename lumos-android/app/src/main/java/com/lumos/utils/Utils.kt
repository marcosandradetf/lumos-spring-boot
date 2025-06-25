package com.lumos.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

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

    fun getFileFromUri(context: Context, uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)!!
        val fileName = "upload_${System.currentTimeMillis()}.jpg"
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








}