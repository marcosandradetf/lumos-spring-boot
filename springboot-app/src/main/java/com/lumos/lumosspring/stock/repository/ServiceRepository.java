package com.lumos.lumosspring.stock.repository;

import com.lumos.lumosspring.stock.entities.MaterialService;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServiceRepository extends JpaRepository<MaterialService, Long> {
    Optional<MaterialService> findByServiceName(String preMeasurementServiceName, Limit limit);
}
