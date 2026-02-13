package com.lumos.lumosspring.installation.repository.premeasurement

import com.lumos.lumosspring.installation.model.premeasurement.PreMeasurementExecutor
import org.springframework.data.repository.CrudRepository

interface PreMeasurementExecutorRepository : CrudRepository<PreMeasurementExecutor, Long> {
}