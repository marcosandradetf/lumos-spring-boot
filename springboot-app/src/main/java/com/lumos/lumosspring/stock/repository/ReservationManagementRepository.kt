package com.lumos.lumosspring.stock.repository

import com.lumos.lumosspring.stock.entities.ReservationManagement
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ReservationManagementRepository : JpaRepository<ReservationManagement, Long> {
    fun existsByStreetsPreMeasurementPreMeasurementIdAndStreetsStep(
        preMeasurementId: Long,
        step: Int
    ): Boolean

    fun findAllByStatusAndStockistIdUser(status: String, uuid: UUID): MutableList<ReservationManagement>

}