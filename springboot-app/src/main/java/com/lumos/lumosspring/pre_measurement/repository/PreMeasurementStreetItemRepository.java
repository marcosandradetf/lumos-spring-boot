package com.lumos.lumosspring.pre_measurement.repository;

import com.lumos.lumosspring.pre_measurement.entities.PreMeasurementStreetItem;
import com.lumos.lumosspring.pre_measurement.entities.PreMeasurementStreet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PreMeasurementStreetItemRepository extends JpaRepository<PreMeasurementStreetItem, Long> {
    List<PreMeasurementStreetItem> findAllByPreMeasurementStreet(PreMeasurementStreet preMeasurementStreet);

    List<PreMeasurementStreetItem> findAllByPreMeasurementStreetItemId(Long id);
}
