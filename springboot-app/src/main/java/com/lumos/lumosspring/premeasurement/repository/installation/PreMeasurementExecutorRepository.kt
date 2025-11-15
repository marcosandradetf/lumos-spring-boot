package com.lumos.lumosspring.premeasurement.repository.installation

import com.lumos.lumosspring.premeasurement.model.PreMeasurementExecutor
import org.springframework.data.repository.CrudRepository

interface PreMeasurementExecutorRepository : CrudRepository<PreMeasurementExecutor, Long> {
}