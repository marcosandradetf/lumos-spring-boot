package com.lumos.lumosspring.pre_measurement.repository;

import com.lumos.lumosspring.pre_measurement.entities.PreMeasurementStreetItem;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface PreMeasurementStreetItemRepository extends CrudRepository<PreMeasurementStreetItem, Long> {
    List<PreMeasurementStreetItem> findAllByPreMeasurementStreetId(Long preMeasurementStreetId);

    List<PreMeasurementStreetItem> findAllByPreMeasurementStreetItemId(Long id);

    List<PreMeasurementStreetItem> findAllByPreMeasurementId(Long preMeasurementId);


}
