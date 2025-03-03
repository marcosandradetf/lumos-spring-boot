package com.lumos.lumosspring.execution.repository;

import com.lumos.lumosspring.execution.entities.PreMeasurementStreetItem;
import com.lumos.lumosspring.execution.entities.PreMeasurementStreet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PreMeasurementStreetItemRepository extends JpaRepository<PreMeasurementStreetItem, Long> {
    List<PreMeasurementStreetItem> findAllByPreMeasurementStreet(PreMeasurementStreet preMeasurementStreet);

    List<PreMeasurementStreetItem> findAllByPreMeasurementStreetItemId(Long id);
}
