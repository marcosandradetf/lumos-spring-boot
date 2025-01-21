package com.lumos.lumosspring.execution.repository;

import com.lumos.lumosspring.execution.entities.PreMeasurement;
import com.lumos.lumosspring.execution.entities.Street;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreMeasurementRepository extends JpaRepository<PreMeasurement, Long> {
}
