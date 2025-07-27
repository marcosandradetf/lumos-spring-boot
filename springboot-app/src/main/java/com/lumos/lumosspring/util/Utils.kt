package com.lumos.lumosspring.util

import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import java.util.UUID

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

}