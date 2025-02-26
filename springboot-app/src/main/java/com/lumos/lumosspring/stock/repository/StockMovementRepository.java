package com.lumos.lumosspring.stock.repository;

import com.lumos.lumosspring.stock.entities.Material;
import com.lumos.lumosspring.stock.entities.MaterialStock;
import com.lumos.lumosspring.stock.entities.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    @Query("SELECT s FROM StockMovement s WHERE s.stockMovementRefresh BETWEEN :startDate AND :endDate")
    List<StockMovement> findApprovedBetweenDates(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);


    @Query("SELECT s FROM StockMovement s WHERE s.materialStock = :material and s.status = :status")
    Optional<StockMovement> findFirstByMaterial(@Param("material") MaterialStock material, @Param("status") StockMovement.Status status);

}
