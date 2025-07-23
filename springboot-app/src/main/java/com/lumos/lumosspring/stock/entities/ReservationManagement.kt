package com.lumos.lumosspring.stock.entities

import com.lumos.lumosspring.util.ReservationStatus
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table
data class ReservationManagement (
    @Id
    val reservationManagementId : Long? = null,

    var description : String? = null,

    var stockistId: UUID,

    var status: String = ReservationStatus.PENDING,

)
