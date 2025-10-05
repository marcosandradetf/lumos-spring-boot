package com.lumos.lumosspring.premeasurement.repository.premeasurement;

import com.lumos.lumosspring.premeasurement.model.PreMeasurementStreet;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PreMeasurementStreetRepository extends CrudRepository<PreMeasurementStreet, Long> {
    List<PreMeasurementStreet> findAllByStreetStatus(String status);

    boolean existsByDevicePreMeasurementStreetId(UUID deviceId);
}