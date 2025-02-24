package com.lumos.lumosspring.execution.repository;

import com.lumos.lumosspring.execution.entities.Item;
import com.lumos.lumosspring.execution.entities.PreMeasurement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {
//    List<Item> findItemsByMeasurement_Status(PreMeasurement.Status measurementStatus);

    @Query("SELECT i FROM Item i WHERE i.preMeasurement.status = :measurementStatus ORDER BY i.preMeasurement.createdAt ASC, i.preMeasurement.city ASC")
    List<Item> findItemsByMeasurement_Status(@Param("measurementStatus") PreMeasurement.Status measurementStatus);

    @Query("SELECT DISTINCT i.preMeasurement.city FROM Item i WHERE i.preMeasurement.status = :measurementStatus")
    List<String> findCities(@Param("measurementStatus") PreMeasurement.Status measurementStatus);

    @Query("SELECT sum(i.itemQuantity), count(i.preMeasurement.address) FROM Item i WHERE i.preMeasurement.status = :measurementStatus AND i.preMeasurement.city = :city")
    List<String> getTotalByCity(@Param("measurementStatus") PreMeasurement.Status measurementStatus, @Param("city") String city);

    @Query("SELECT i " +
            "FROM Item i WHERE i.preMeasurement.preMeasurementId = :measurementId " +
            "ORDER BY i.preMeasurement.createdAt ASC, i.preMeasurement.city ASC")
    Item findCitiesWithStreets(@Param("measurementId") long measurementId);


    List<Item> findItemsByPreMeasurement_PreMeasurementId(long measurementId);



}
