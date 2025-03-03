package com.lumos.lumosspring.execution.repository;

import com.lumos.lumosspring.execution.entities.PreMeasurementStreet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PreMeasurementStreetRepository extends JpaRepository<PreMeasurementStreet, Long> {
    List<PreMeasurementStreet> findAllByStatus(PreMeasurementStreet.Status status);
}
