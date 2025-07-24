package com.lumos.lumosspring.stock.repository;

import com.lumos.lumosspring.stock.entities.StockMovement;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockMovementRepository extends CrudRepository<StockMovement, Long> {
    @Query("""
        SELECT s.* FROM\s
        stock_movement s WHERE s.stock_movement_refresh BETWEEN :startDate AND :endDate
   \s""")
    List<StockMovement> findApprovedBetweenDates(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);


    @Query("""
        SELECT s.*\s
        FROM stock_movement s\s
        WHERE s.material_stock_id = :materialStockId\s
          and s.status = :status
        LIMIT 1
   \s""")
    Optional<StockMovement> findFirstByMaterial(@Param("materialStockId") Long materialStockId, @Param("status") String status);

    List<StockMovement> findAllByStatus(String status);
}
