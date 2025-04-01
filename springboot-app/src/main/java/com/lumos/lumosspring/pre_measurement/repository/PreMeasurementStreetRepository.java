package com.lumos.lumosspring.pre_measurement.repository;

import com.lumos.lumosspring.pre_measurement.entities.PreMeasurementStreet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PreMeasurementStreetRepository extends JpaRepository<PreMeasurementStreet, Long> {
    List<PreMeasurementStreet> findAllByStreetStatus(String status);
}
