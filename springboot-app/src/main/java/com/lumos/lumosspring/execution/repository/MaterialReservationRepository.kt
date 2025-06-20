package com.lumos.lumosspring.execution.repository

import com.lumos.lumosspring.execution.entities.MaterialReservation
import com.lumos.lumosspring.util.ReservationStatus
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository


interface MaterialReservationRepository : CrudRepository<MaterialReservation, Long> {

}