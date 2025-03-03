package com.lumos.lumosspring.execution.repository;

import com.lumos.lumosspring.execution.entities.PreMeasurement;
import com.lumos.lumosspring.execution.entities.PreMeasurementStreet;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PreMeasurementRepository extends JpaRepository<PreMeasurement, Long> {
    PreMeasurement findFirstByCityAndCreatedAt(String city, Instant createdAt);

    Optional<PreMeasurement> getTopByCityAndStatusOrderByCreatedAtDesc(String city, PreMeasurement.Status status);

    List<PreMeasurement> findByCity(String city);

    @EntityGraph(attributePaths = {"createdBy", "streets.items"})
    List<PreMeasurement> findAllByStatusOrderByCreatedAtAsc(PreMeasurement.Status status);
}
