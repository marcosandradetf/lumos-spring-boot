package com.lumos.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.location.LocationManager
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.ContextCompat
import com.lumos.api.RequestResult
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import androidx.core.graphics.scale
import com.lumos.midleware.SecureStorage


object Utils {

    fun Context.findActivity(): Activity? {
        var ctx = this
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }


    val dateTime: Instant
        get() {
            val date = ZonedDateTime.now(
                ZoneId.of("America/Sao_Paulo")
            )

            return date.toInstant()
        }

//    private fun parseTimestamptzToInstant(raw: String): Instant {
//        // PostgreSQL retorna algo como "2025-07-27 22:34:10.952689+00"
//        // Ajuste para ISO 8601: "2025-07-27T22:34:10.952689+00:00"
//        val formatted = raw
//            .replace(" ", "T")
//            .replace(Regex("""\+(\d{2})$"""), "+$1:00")  // transforma +00 → +00:00
//
//        return Instant.parse(formatted)
//    }

    private fun parseTimestamptzToInstant(raw: String): Instant {
        // 1) tenta ISO 8601 direto (ex: "2025-08-28T10:03:07Z")
        try {
            return Instant.parse(raw)
        } catch (_: Exception) {
        }

        // 2) normaliza frações de segundo (se tiver .00749 → vira .007490)
        val normalized = raw.replace(
            Regex("""\.(\d{3,6})""")
        ) { matchResult ->
            val fraction = matchResult.groupValues[1]
            "." + fraction.padEnd(6, '0') // sempre 6 dígitos
        }

        // 3) formatadores possíveis para timestamptz
        val formatters = listOf(
            "yyyy-MM-dd HH:mm:ss.SSSSSSX",   // ex: -03
            "yyyy-MM-dd HH:mm:ss.SSSSSSXX",  // ex: -0300
            "yyyy-MM-dd HH:mm:ss.SSSSSSXXX", // ex: -03:00
            "yyyy-MM-dd HH:mm:ssX",          // sem fração, offset -03
            "yyyy-MM-dd HH:mm:ssXX",         // sem fração, offset -0300
            "yyyy-MM-dd HH:mm:ssXXX"         // sem fração, offset -03:00
        ).map {
            DateTimeFormatter.ofPattern(it)
                .withLocale(Locale.US)
                .withZone(ZoneId.of("UTC"))
        }

        for (formatter in formatters) {
            try {
                return OffsetDateTime.parse(normalized, formatter).toInstant()
            } catch (_: Exception) {
            }
        }

        // 4) fallback → nunca crasha
        return Instant.EPOCH
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
        quality: Int = 75,
        maxSize: Int = 1280
    ): ByteArray? {
        return try {
            // 1. Ler EXIF
            val exif = ExifInterface(context.contentResolver.openInputStream(imageUri)!!)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            // 2. Carregar bitmap
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) return null

            // 3. Corrigir rotação conforme EXIF
            val rotated = rotateBitmapIfNeeded(bitmap, orientation)

            // 4. Redimensionar se necessário
            val resized = resizeBitmap(rotated, maxSize)

            if (rotated != bitmap) bitmap.recycle()

            // 5. Comprimir
            val output = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, quality, output)

            resized.recycle()

            output.toByteArray()

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun rotateBitmapIfNeeded(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Se já for menor que maxSize, não redimensiona
        if (width <= maxSize && height <= maxSize) return bitmap

        val ratio = width.toFloat() / height.toFloat()

        val newWidth: Int
        val newHeight: Int

        if (ratio > 1) {
            // Horizontal
            newWidth = maxSize
            newHeight = (maxSize / ratio).toInt()
        } else {
            // Vertical
            newHeight = maxSize
            newWidth = (maxSize * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
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

    fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasNumber(address: String): Boolean {
        val hasNumber = Regex("""\d+""").containsMatchIn(address)
        val hasSN =
            Regex("""(?i)\bS[\./\\]?\s?N\b""").containsMatchIn(address)

        return hasNumber || hasSN
    }

    fun checkResponse(response: RequestResult<Unit>): String? {
        return when (response) {
            is RequestResult.Success -> {
                null
            }

            is RequestResult.SuccessEmptyBody -> {
                null
            }

            is RequestResult.NoInternet -> {
                "Sem internet - Falha na comunicação com o servidor. Reconecte e tente novamente!"
            }

            is RequestResult.Timeout -> {
                "Timeout - Solicitação excedeu o tempo de espera. Tente novamente!"

            }

            is RequestResult.ServerError -> {
                response.message
            }

            is RequestResult.UnknownError -> {
                response.error.message ?: response.error.toString()
            }

        }
    }

    fun translateStatus(executionStatus: String): String {
        return when (executionStatus) {
            "PENDING" -> "PENDENTE"
            "IN_PROGRESS" -> "EM ANDAMENTO"
            "FINISHED", "FINISH" -> "FINALIZADO"
            else -> "DESCONHECIDO"
        }
    }

    fun deletePhoto(context: Context, uri: Uri) {
        try {
            val file = File(uri.path ?: return)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isStaleCheckTeam(secureStorage: SecureStorage): Boolean {
        val TWELVE_HOURS = 12 * 60 * 60 * 1000L
        val now = System.currentTimeMillis()
        val lastTeamCheck = secureStorage.getLastTeamCheck()

        return now >= lastTeamCheck && (now - lastTeamCheck > TWELVE_HOURS)
    }

}

