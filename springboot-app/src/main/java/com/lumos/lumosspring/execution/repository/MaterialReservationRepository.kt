package com.lumos.lumosspring.execution.repository

import com.lumos.lumosspring.execution.dto.Reserve
import com.lumos.lumosspring.execution.entities.MaterialReservation
import com.lumos.lumosspring.pre_measurement.entities.PreMeasurementStreet
import com.lumos.lumosspring.stock.entities.MaterialStock
import com.lumos.lumosspring.util.ReservationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional


interface MaterialReservationRepository : JpaRepository<MaterialReservation, Long> {
    @Query(
        "SELECT m FROM MaterialReservation m " +
                "WHERE m.street is not null " +
                "AND m.street.preMeasurementStreetId in :streetIds AND m.status in :statuses"
    )
    fun findAllByStreetInStreetId(
        streetIds: List<Long>,
        statuses: List<String> = listOf(
            ReservationStatus.PENDING,
            ReservationStatus.COLLECTED,
            ReservationStatus.APPROVED
        )
    ): List<MaterialReservation>

}