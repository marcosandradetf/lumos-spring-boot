package com.lumos.lumosspring.lead.controller

import com.lumos.lumosspring.lead.service.LeadService
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
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
        @RequestParam("firstName") firstName: String,
        @RequestParam("lastName") lastName: String,
        @RequestParam("email") email: String,
        @RequestParam("password") password: String,
        @RequestParam("company", defaultValue = "") company: String,
        @RequestParam("teamSize", defaultValue = "") teamSize: String,
        @RequestParam("message", required = false) message: String?,
        @RequestParam("phone", defaultValue = "") phone: String,
        @RequestParam("planName", defaultValue = "Profissional") planName: String,
        @RequestParam("useTrial", defaultValue = "true") useTrial: Boolean,
        response: HttpServletResponse,
    ) = leadService.startFreeTest(
        firstName = firstName,
        lastName = lastName,
        email = email,
        password = password,
        response = response,
        phone = phone,
        company = company,
        teamSize = teamSize,
        message = message,
        planName = planName,
        useTrial = useTrial,
        isMobile = false,
    )
}
