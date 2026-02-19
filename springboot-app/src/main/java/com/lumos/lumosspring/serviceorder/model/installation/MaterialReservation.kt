package com.lumos.lumosspring.serviceorder.model.installation

import com.lumos.lumosspring.authentication.model.TenantEntity
import com.lumos.lumosspring.util.ReservationStatus
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Table("material_reservation")
data class MaterialReservation(
    @Id
    var materialIdReservation: Long? = null,
    var description: String? = null,
    var centralMaterialStockId: Long? = null,
    var truckMaterialStockId: Long,
    var preMeasurementId: Long? = null,
    var directExecutionId: Long? = null,
    var contractItemId: Long,
    var reservedQuantity: BigDecimal,
    var quantityCompleted: BigDecimal = BigDecimal.ZERO,
    var status: String = ReservationStatus.PENDING,
    var teamId: Long,

    var createdAt: Instant,
    var createdBy: UUID,

    var collectedAt: Instant? = null,
    var releasedBy: UUID? = null,
    var finishedAt: Instant? = null,
) : TenantEntity()