package com.lumos.lumosspring.stock.order.repository

import com.lumos.lumosspring.stock.order.model.MaterialReservation
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface MaterialReservationRepository : CrudRepository<MaterialReservation, Long> {

}