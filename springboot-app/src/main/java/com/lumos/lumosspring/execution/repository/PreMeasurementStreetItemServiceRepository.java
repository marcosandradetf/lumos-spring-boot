package com.lumos.lumosspring.execution.repository;

import com.lumos.lumosspring.execution.entities.PreMeasurementStreet;
import com.lumos.lumosspring.execution.entities.PreMeasurementStreetItem;
import com.lumos.lumosspring.execution.entities.PreMeasurementStreetItemService;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PreMeasurementStreetItemServiceRepository extends JpaRepository<PreMeasurementStreetItemService, Long> {

}
