package com.lumos.lumosspring.execution.repository

import com.lumos.lumosspring.execution.entities.MaterialReservation
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional


interface MaterialReservationRepository : JpaRepository<MaterialReservation, Long> {
    fun findAllByTeam_IdTeam(teamId: Long): Optional<List<MaterialReservation>>
}