package com.lumos.lumosspring.stock.order.repository

import com.lumos.lumosspring.stock.order.model.ReservationManagement
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ReservationManagementRepository : CrudRepository<ReservationManagement, Long> {
//    fun existsByStreetsPreMeasurementPreMeasurementIdAndStreetsStep(
//        preMeasurementId: Long,
//        step: Int
//    ): Boolean
//
//    fun findAllByStatusAndStockistUserId(status: String, uuid: UUID): MutableList<ReservationManagement>

}