package com.lumos.lumosspring.execution.repository

import com.lumos.lumosspring.execution.entities.MaterialReservation
import org.springframework.data.jpa.repository.JpaRepository

interface MaterialReservationRepository : JpaRepository<MaterialReservation, Long> {
}