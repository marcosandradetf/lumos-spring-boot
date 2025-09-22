package com.lumos.lumosspring.pre_measurement.repository;


import com.lumos.lumosspring.pre_measurement.dto.response.PreMeasurementResponseDTO;
import com.lumos.lumosspring.pre_measurement.entities.PreMeasurement;
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

}
