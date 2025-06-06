package com.lumos.lumosspring.pre_measurement.repository;

import com.lumos.lumosspring.contract.entities.Contract;
import com.lumos.lumosspring.pre_measurement.entities.PreMeasurement;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PreMeasurementRepository extends JpaRepository<PreMeasurement, Long> {
    List<PreMeasurement> findByCity(String city);

    @EntityGraph(attributePaths = {"streets.createdBy", "streets.items"})
    PreMeasurement findByPreMeasurementIdAndStatus(Long preMeasurementId, String status);

    @EntityGraph(attributePaths = {"streets.createdBy", "streets.items"})
    PreMeasurement findByPreMeasurementId(Long preMeasurementId);

    Optional<PreMeasurement> findByContract_ContractId(Long contractId);

}
