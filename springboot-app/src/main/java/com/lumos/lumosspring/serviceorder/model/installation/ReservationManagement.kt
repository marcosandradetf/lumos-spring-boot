package com.lumos.lumosspring.serviceorder.model.installation

import com.lumos.lumosspring.authentication.model.TenantEntity
import com.lumos.lumosspring.util.ReservationStatus
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table
data class ReservationManagement (
    @Id
    val reservationManagementId : Long? = null,
    var description : String? = null,
    var stockistId: UUID,
    var status: String = ReservationStatus.PENDING,

    ) : TenantEntity()