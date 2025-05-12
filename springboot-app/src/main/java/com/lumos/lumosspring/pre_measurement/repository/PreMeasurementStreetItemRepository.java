package com.lumos.lumosspring.pre_measurement.repository;

import com.lumos.lumosspring.pre_measurement.entities.PreMeasurement;
import com.lumos.lumosspring.pre_measurement.entities.PreMeasurementStreetItem;
import com.lumos.lumosspring.pre_measurement.entities.PreMeasurementStreet;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PreMeasurementStreetItemRepository extends JpaRepository<PreMeasurementStreetItem, Long> {
    List<PreMeasurementStreetItem> findAllByPreMeasurementStreet(PreMeasurementStreet preMeasurementStreet);

    List<PreMeasurementStreetItem> findAllByPreMeasurementStreetItemId(Long id);

    List<PreMeasurementStreetItem> findAllByPreMeasurement(PreMeasurement preMeasurement);

    @Modifying
    @Transactional
    @Query("DELETE FROM PreMeasurementStreetItem pmsi WHERE pmsi.preMeasurementStreet.preMeasurementStreetId in (:streetsIds)")
    void deleteByStreet(@Param("streetsIds") List<Long> streetsIds);

}
