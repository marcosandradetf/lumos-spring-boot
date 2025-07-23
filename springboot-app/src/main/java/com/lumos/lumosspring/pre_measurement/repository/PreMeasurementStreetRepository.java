package com.lumos.lumosspring.pre_measurement.repository;

import com.lumos.lumosspring.execution.dto.ExecutionPartial;
import com.lumos.lumosspring.pre_measurement.entities.PreMeasurementStreet;
import com.lumos.lumosspring.team.entities.Team;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface PreMeasurementStreetRepository extends CrudRepository<PreMeasurementStreet, Long> {
    List<PreMeasurementStreet> findAllByStreetStatus(String status);

    List<PreMeasurementStreet> findByTeamId(Long teamId);

    boolean existsByDeviceIdAndDeviceStreetId(String deviceId, long deviceStreetId);
}