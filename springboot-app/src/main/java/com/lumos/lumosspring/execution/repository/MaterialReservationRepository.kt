package com.lumos.lumosspring.execution.repository

import com.lumos.lumosspring.execution.entities.MaterialReservation
import com.lumos.lumosspring.util.ReservationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query


interface MaterialReservationRepository : JpaRepository<MaterialReservation, Long> {
    @Query(
        "SELECT m FROM MaterialReservation m " +
                "WHERE m.street is not null " +
                "AND m.street.preMeasurementStreetId in :streetIds AND m.status in :statuses "
    )
    fun findAllToIndirectExecution(
        streetIds: List<Long>,
        statuses: List<String> = listOf(
            ReservationStatus.PENDING,
            ReservationStatus.COLLECTED,
            ReservationStatus.APPROVED
        )
    ): List<MaterialReservation>

    @Query(
        """
                SELECT DISTINCT m FROM MaterialReservation m
                JOIN FETCH m.directExecution de
                JOIN FETCH de.contract c
                LEFT JOIN FETCH m.materialStock ms
                LEFT JOIN FETCH ms.deposit d
                LEFT JOIN FETCH d.stockists s
                LEFT JOIN FETCH s.user u
                LEFT JOIN FETCH m.street st
                WHERE m.directExecution IS NOT NULL
                AND m.status IN :statuses
                AND m.team.idTeam IN :teamIds
            """
    )
    fun findAllToDirectExecution(
        teamIds: List<Long>,
        statuses: List<String> = listOf(
            ReservationStatus.PENDING,
            ReservationStatus.COLLECTED,
            ReservationStatus.APPROVED
        )
    ): List<MaterialReservation>


}