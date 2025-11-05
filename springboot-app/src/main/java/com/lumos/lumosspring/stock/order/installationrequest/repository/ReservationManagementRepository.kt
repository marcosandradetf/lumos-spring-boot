package com.lumos.lumosspring.stock.order.installationrequest.repository

import com.lumos.lumosspring.stock.order.installationrequest.model.ReservationManagement
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ReservationManagementRepository : CrudRepository<ReservationManagement, Long> {
}