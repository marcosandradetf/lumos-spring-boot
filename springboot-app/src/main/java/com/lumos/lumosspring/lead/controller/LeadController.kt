package com.lumos.lumosspring.lead.controller

import com.lumos.lumosspring.lead.service.LeadService
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/public")
class LeadController(
    private val leadService: LeadService,
) {
    @PostMapping(value = ["/demo-request"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun demoRequest(
        @RequestParam("firstName") firstName: String,
        @RequestParam("lastName") lastName: String,
        @RequestParam("email") email: String,
        @RequestParam("company") company: String,
        @RequestParam("teamSize") teamSize: String,
        @RequestParam("message") message: String?,
        @RequestParam("phone") phone: String,
    ) = leadService.demoOrTestRequest(
        type = "DEMO",
        firstName = firstName,
        lastName = lastName,
        email = email,
        company = company,
        teamSize = teamSize,
        message = message,
        phone = phone,
    )

    /**
     * Cria conta trial (tenant + usuário + subscription), grava lead e devolve tokens como no login (web: cookie refresh + access no body).
     */
    @PostMapping(value = ["/free-test"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun freeTest(
        @RequestParam("_gotcha") gotcha: String,
        @RequestParam("planName", defaultValue = "Profissional") planName: String,
        @RequestParam("useTrial", defaultValue = "true") useTrial: Boolean,
        @RequestParam("firstName") firstName: String,
        @RequestParam("lastName") lastName: String,
        @RequestParam("phone", defaultValue = "") phone: String,
        @RequestParam("email") email: String,
        @RequestParam("company", defaultValue = "") company: String,
        @RequestParam("cnpj", defaultValue = "") cnpj: String,
        @RequestParam("teamSize", defaultValue = "") teamSize: String,
        @RequestParam("operationFocus", defaultValue = "") operationFocus: String,
        @RequestParam("currentMoment", defaultValue = "") currentMoment: String,
        @RequestParam("message", required = false) message: String?,
        @RequestParam("password") password: String,
        response: HttpServletResponse,
    ): ResponseEntity<*> {
        if (gotcha.isNotEmpty()) {
            return ResponseEntity.status(403).body("")
        }

        return leadService.startFreeTest(
            useTrial = useTrial,
            firstName = firstName,
            lastName = lastName,
            phone = phone,
            email = email,
            company = company,
            cnpj = cnpj,
            teamSize = teamSize,
            operationFocus = operationFocus,
            currentMoment = currentMoment,
            message = message,
            password = password,
            response = response,
            isMobile = false,
        )
    }
}
