package com.lumos.lumosspring.execution.repository

import com.lumos.lumosspring.execution.entities.MaterialReservation
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Indexed

@Indexed
interface MaterialReservationRepository : CrudRepository<MaterialReservation, Long> {

}