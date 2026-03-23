package com.lumos.lumosspring.lead.controller

import com.lumos.lumosspring.lead.service.LeadService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/public")
class LeadController(
    private val leadService: LeadService
) {
    @PostMapping(value = ["/demo-request"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun demoRequest(
        @RequestParam("firstName") firstName: String,
        @RequestParam("lastName") lastName: String,
        @RequestParam("email") email: String,
        @RequestParam("company") company: String,
        @RequestParam("teamSize") teamSize: String,
        @RequestParam("message") message: String?,
        @RequestParam("phone") phone: String
    ) = leadService.demoOrTestRequest(
        type = "DEMO",
        firstName = firstName,
        lastName = lastName,
        email = email,
        company = company,
        teamSize = teamSize,
        message = message,
        phone = phone
    )

    @PostMapping(value = ["/free-test"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun freeTest(
        @RequestParam("firstName") firstName: String,
        @RequestParam("lastName") lastName: String,
        @RequestParam("email") email: String,
        @RequestParam("company") company: String,
        @RequestParam("teamSize") teamSize: String,
        @RequestParam("message") message: String?,
        @RequestParam("phone") phone: String
    ) = leadService.demoOrTestRequest(
        type = "TEST",
        firstName = firstName,
        lastName = lastName,
        email = email,
        company = company,
        teamSize = teamSize,
        message = message,
        phone = phone
    )
}