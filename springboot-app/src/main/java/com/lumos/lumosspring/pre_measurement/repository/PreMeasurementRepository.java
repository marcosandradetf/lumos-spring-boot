package com.lumos.lumosspring.pre_measurement.repository;

import com.lumos.lumosspring.pre_measurement.entities.PreMeasurement;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PreMeasurementRepository extends JpaRepository<PreMeasurement, Long> {
    PreMeasurement findFirstByCityAndCreatedAt(String city, Instant createdAt);

    Optional<PreMeasurement> getTopByCityAndStatusOrderByCreatedAtDesc(String city, String status);

    List<PreMeasurement> findByCity(String city);

    @EntityGraph(attributePaths = {"createdBy", "streets.items"})
    List<PreMeasurement> findAllByStatusOrderByCreatedAtAsc(String status);

    @EntityGraph(attributePaths = {"createdBy", "streets.items"})
    PreMeasurement findByPreMeasurementIdAndStatus(Long preMeasurementId, String status);



}
