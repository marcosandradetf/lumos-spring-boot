package com.lumos.lumosspring.stock.materialstock.repository;

import com.lumos.lumosspring.stock.materialstock.dto.StockMovementResponse;
import com.lumos.lumosspring.stock.materialstock.model.StockMovement;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockMovementRepository extends CrudRepository<StockMovement, Long> {
    @Query("""
                SELECT s.*
                FROM stock_movement s
                WHERE s.material_stock_id = :materialStockId
                    and s.status = :status
                LIMIT 1
            """)
    Optional<StockMovement> findFirstByMaterial(@Param("materialStockId") Long materialStockId, @Param("status") String status);

    @Query("""
                select
                    s.stock_movement_id as id,
                    s.stock_movement_description as description,
                    m.material_name as material_name,
                    s.input_quantity,
                    m.buy_unit,
                    m.request_unit,
                    s.price_total,
                    s.price_per_item,
                    d.deposit_name as deposit,
                    u.name || ' ' || u.last_name as responsible,
                    s.stock_movement_refresh as date_of,
                    s.total_quantity,
                    s.quantity_package
                from stock_movement s
                join material_stock ms on ms.material_id_stock = s.material_stock_id
                join deposit d on d.id_deposit = ms.deposit_id
                join material m on m.id_material = ms.material_id
                join app_user u on u.user_id = s.user_created_id_user
                where s.status = :status and s.tenant_id = :tenantId
            """)
    List<StockMovementResponse> findAllByStatus(String status, UUID tenantId);

    @Query("""
                select
                    s.stock_movement_id as id,
                    s.stock_movement_description as description,
                    m.material_name as material_name,
                    s.input_quantity,
                    m.buy_unit,
                    m.request_unit,
                    s.price_total,
                    s.price_per_item,
                    d.deposit_name as deposit,
                    u2.name || ' ' || u2.last_name as responsible,
                    s.stock_movement_refresh as date_of,
                    s.total_quantity,
                    s.quantity_package
                from stock_movement s
                join material_stock ms on ms.material_id_stock = s.material_stock_id
                join deposit d on d.id_deposit = ms.deposit_id
                join material m on m.id_material = ms.material_id
                join app_user u2 on u2.user_id = s.user_finished_id_user
                WHERE s.stock_movement_refresh BETWEEN :startDate AND :endDate
                    AND s.status = :status  and s.tenant_id = :tenantId
            """)
    List<StockMovementResponse> findApprovedBetweenDates(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate, String status, UUID tenantId);
}
