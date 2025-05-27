package com.lumos.lumosspring.execution.repository

import com.lumos.lumosspring.execution.entities.MaterialReservation
import com.lumos.lumosspring.pre_measurement.entities.PreMeasurementStreet
import com.lumos.lumosspring.stock.entities.MaterialStock
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional


interface MaterialReservationRepository : JpaRepository<MaterialReservation, Long> {
    fun findAllByStreetPreMeasurementStreetId(streetId: Long): Optional<List<MaterialReservation>>

    fun findAllByStreet(street: PreMeasurementStreet): List<MaterialReservation>

}