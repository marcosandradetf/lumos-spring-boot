package com.lumos.lumosspring.stock.repository

import com.lumos.lumosspring.stock.entities.ReservationManagement
import org.springframework.data.jpa.repository.JpaRepository

interface ReservationManagementRepository : JpaRepository<ReservationManagement, Long> {
    fun existsByStreetsPreMeasurementPreMeasurementIdAndStreetsStep(
        preMeasurementId: Long,
        step: Int
    ): Boolean

    fun findAllByStatus(status: String): MutableList<ReservationManagement>


}