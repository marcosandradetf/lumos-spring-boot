package com.lumos.lumosspring.serviceorder.repository.installation

import com.lumos.lumosspring.stock.history.model.MaterialReservation
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface MaterialReservationRepository : CrudRepository<MaterialReservation, Long> {

}