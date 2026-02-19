package com.lumos.lumosspring.serviceorder.repository.installation

import com.lumos.lumosspring.serviceorder.model.installation.MaterialReservation
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface MaterialReservationRepository : CrudRepository<MaterialReservation, Long> {
    fun deleteByDirectExecutionId(directExecutionId: Long)
    fun deleteByPreMeasurementId(preMeasurementId: Long)

}