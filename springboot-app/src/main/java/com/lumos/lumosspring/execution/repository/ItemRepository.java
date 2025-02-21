package com.lumos.lumosspring.execution.repository;

import com.lumos.lumosspring.execution.controller.dto.PreMeasurementByCityDTO;
import com.lumos.lumosspring.execution.entities.Item;
import com.lumos.lumosspring.execution.entities.PreMeasurement;
import jakarta.persistence.OrderBy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {
//    List<Item> findItemsByMeasurement_Status(PreMeasurement.Status measurementStatus);

    @Query("SELECT i FROM Item i WHERE i.measurement.status = :measurementStatus ORDER BY i.measurement.createdAt ASC, i.measurement.city ASC")
    List<Item> findItemsByMeasurement_Status(@Param("measurementStatus") PreMeasurement.Status measurementStatus);

    @Query("SELECT DISTINCT i.measurement.city FROM Item i WHERE i.measurement.status = :measurementStatus")
    List<String> findCities(@Param("measurementStatus") PreMeasurement.Status measurementStatus);

    @Query("SELECT sum(i.itemQuantity), count(i.measurement.address) FROM Item i WHERE i.measurement.status = :measurementStatus AND i.measurement.city = :city")
    List<String> getTotalByCity(@Param("measurementStatus") PreMeasurement.Status measurementStatus, @Param("city") String city);

    @Query("SELECT i " +
            "FROM Item i WHERE i.measurement.measurementId = :measurementId " +
            "ORDER BY i.measurement.createdAt ASC, i.measurement.city ASC")
    Item findCitiesWithStreets(@Param("measurementId") long measurementId);
}
