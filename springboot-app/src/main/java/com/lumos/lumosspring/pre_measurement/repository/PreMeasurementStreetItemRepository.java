package com.lumos.lumosspring.pre_measurement.repository;

import com.lumos.lumosspring.pre_measurement.entities.PreMeasurementStreetItem;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PreMeasurementStreetItemRepository extends CrudRepository<PreMeasurementStreetItem, Long> {
    List<PreMeasurementStreetItem> findAllByPreMeasurementStreetId(Long preMeasurementStreetId);

    List<PreMeasurementStreetItem> findAllByPreMeasurementStreetItemId(Long id);

    List<PreMeasurementStreetItem> findAllByPreMeasurementId(Long preMeasurementId);


}
