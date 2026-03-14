package com.lumos.lumosspring.authentication.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.util.UUID

open class TenantEntity {
    @JsonIgnore
    lateinit var tenantId: UUID
}
