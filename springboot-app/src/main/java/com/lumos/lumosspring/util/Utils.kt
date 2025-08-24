package com.lumos.lumosspring.util

import org.springframework.http.*
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

object Utils {
    fun getCurrentUserId(): UUID {
        val authentication = SecurityContextHolder.getContext().authentication
        return UUID.fromString(authentication.name)
    }

    fun String.replacePlaceholders(values: Map<String, String>): String {
        var result = this
        values.forEach { (key, value) ->
            result = result.replace("{{${key}}}", value)
        }
        return result
    }

    fun sendHtmlToPuppeteer(templateHtml: String, orientation: String = "landscape"): ByteArray? {
        val restTemplate = RestTemplate()

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }

        val body = mapOf(
            "html" to templateHtml,
            "orientation" to orientation // <-- aqui está a mágica
        )

        val request = HttpEntity(body, headers)

        val url = UriComponentsBuilder
            .fromHttpUrl("http://puppeteer-service:3000/generate-pdf")
            .build()
            .toUri()

        val response: ResponseEntity<ByteArray> = restTemplate.exchange(
            url,
            HttpMethod.POST,
            request,
            ByteArray::class.java
        )

        return if (response.statusCode.is2xxSuccessful) {
            response.body
        } else {
            null
        }
    }


    fun convertToSaoPauloZoned(instant: Instant): ZonedDateTime {
        return instant.atZone(ZoneId.of("America/Sao_Paulo"))
    }

    fun convertToSaoPauloLocal(instant: Instant): LocalDateTime {
        return instant.atZone(ZoneId.of("America/Sao_Paulo")).toLocalDateTime()
    }

    fun formatMoney(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")) // Forma moderna
        return format.format(amount)
    }

    fun uuidToShortCodeWithPrefix(prefix: String, uuid: UUID, length: Int = 10): String {
        val mostSigBits = uuid.mostSignificantBits
        val base36 = java.lang.Long.toUnsignedString(mostSigBits, 36).uppercase()

        // Ajusta tamanho fixo e adiciona prefixo REQ-
        val code = base36.padStart(length, '0').takeLast(length)
        return "$prefix-$code"
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    class BusinessException(message: String?) : RuntimeException(message)

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    class BusinessExceptionObjectResponse(
        val data: Any
    ) : RuntimeException()


    @RestControllerAdvice
    class GlobalExceptionHandler {

        @ExceptionHandler(BusinessException::class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        fun handleBusinessException(ex: BusinessException): Map<String, String> {
            return mapOf("error" to (ex.message ?: "Erro de negócio"))
        }

        @ExceptionHandler(BusinessExceptionObjectResponse::class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        fun handleBusinessExceptionObject(ex: BusinessExceptionObjectResponse): Any {
            return ex.data  // pode ser List, Map, DTO, etc.
        }
    }





}