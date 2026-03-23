package com.lumos.lumosspring.lead.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table
class LeadRequest {
    @Id
    var id: Long? = null

    var type: String = ""

    var firstName: String = ""
    var lastName: String = ""

    var email: String = ""
    var phone: String = ""
    var company: String = ""
    var teamSize: String = ""

    var createdAt: Instant = Instant.now()
    var status: String = "RECEIVED"
    var source: String = "LANDING-PAGE"
    var message: String? = null
    var priority: Int = 0
}