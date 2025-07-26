package com.lumos.lumosspring.pre_measurement.repository;

import com.lumos.lumosspring.pre_measurement.entities.PreMeasurementStreet;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PreMeasurementStreetRepository extends CrudRepository<PreMeasurementStreet, Long> {
    List<PreMeasurementStreet> findAllByStreetStatus(String status);

    List<PreMeasurementStreet> findByTeamId(Long teamId);

    boolean existsByDeviceIdAndDeviceStreetId(String deviceId, long deviceStreetId);
}