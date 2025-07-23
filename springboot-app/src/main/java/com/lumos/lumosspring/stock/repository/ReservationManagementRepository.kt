package com.lumos.lumosspring.stock.repository

import com.lumos.lumosspring.stock.entities.ReservationManagement
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface ReservationManagementRepository : CrudRepository<ReservationManagement, Long> {
//    fun existsByStreetsPreMeasurementPreMeasurementIdAndStreetsStep(
//        preMeasurementId: Long,
//        step: Int
//    ): Boolean
//
//    fun findAllByStatusAndStockistUserId(status: String, uuid: UUID): MutableList<ReservationManagement>

}