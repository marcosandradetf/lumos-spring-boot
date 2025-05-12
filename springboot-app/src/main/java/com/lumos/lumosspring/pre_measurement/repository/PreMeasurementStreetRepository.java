package com.lumos.lumosspring.pre_measurement.repository;

import com.lumos.lumosspring.pre_measurement.entities.PreMeasurementStreet;
import com.lumos.lumosspring.team.entities.Team;
import jakarta.transaction.Transactional;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PreMeasurementStreetRepository extends JpaRepository<PreMeasurementStreet, Long> {
    List<PreMeasurementStreet> findAllByStreetStatus(String status);

    List<PreMeasurementStreet> findByTeam(Team team);

    @Modifying
    @Transactional
    @Query("DELETE FROM PreMeasurementStreet pms WHERE pms.preMeasurementStreetId in (:streetsIds)")
    void deleteByStreet(@Param("streetsIds") List<Long> streetsIds);

    @Query("SELECT pms from PreMeasurementStreet pms WHERE pms.preMeasurementStreetId in (:streetsIds)")
    List<PreMeasurementStreet> findByIds(@Param("streetsIds") List<Long> preMeasurementStreetIds);
}
