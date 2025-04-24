package com.lumos.lumosspring.execution.repository

import com.lumos.lumosspring.execution.entities.MaterialReservation
import com.lumos.lumosspring.pre_measurement.entities.PreMeasurementStreet
import com.lumos.lumosspring.stock.entities.MaterialStock
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional


interface MaterialReservationRepository : JpaRepository<MaterialReservation, Long> {
    fun findAllByStreetPreMeasurementStreetId(streetId: Long): Optional<List<MaterialReservation>>

    fun findAllByStreet(street: PreMeasurementStreet): MutableList<MaterialReservation>?
    fun findAllByFirstDepositCityOrSecondDepositCity(
        firstDepositCity: MaterialStock,
        secondDepositCity: MaterialStock
    ): MutableList<MaterialReservation>
}