package com.lumos.lumosspring.premeasurement.repository;


import com.lumos.lumosspring.installation.model.premeasurement.PreMeasurement;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PreMeasurementRepository extends CrudRepository<PreMeasurement, Long> {
    List<PreMeasurement> findByCity(String city);

    PreMeasurement findByPreMeasurementIdAndStatus(Long preMeasurementId, String status);

    PreMeasurement findByPreMeasurementId(Long preMeasurementId);

    Optional<PreMeasurement> findByContractId(Long contractId);

    Optional<PreMeasurement> findByDevicePreMeasurementId(UUID id);

    @Query("SELECT status from pre_measurement where pre_measurement_id = :preMeasurementId")
    String getStatus(Long preMeasurementId);

    @Query("SELECT case when reservation_management_id is null then false else true end from pre_measurement where pre_measurement_id = :preMeasurementId")
    boolean hasManagement(long preMeasurementId);
}
