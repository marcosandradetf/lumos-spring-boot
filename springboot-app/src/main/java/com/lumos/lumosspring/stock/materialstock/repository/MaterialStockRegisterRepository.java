package com.lumos.lumosspring.stock.materialstock.repository;

import com.lumos.lumosspring.stock.materialstock.model.MaterialStock;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MaterialStockRegisterRepository extends CrudRepository<MaterialStock, Long> {
    @Modifying
    @Query(
        """
            UPDATE material_stock
            SET stock_quantity = stock_quantity - :quantity,
                stock_available = stock_available - :quantity
            WHERE material_id_stock = :materialStockId
        """
    )
    void debitStock(BigDecimal quantity, Long materialStockId);

    @Modifying
    @Query(
            """
                insert into material_stock (
                    buy_unit,
                    cost_per_item,
                    cost_price,
                    inactive,
                    request_unit,
                    stock_available,
                    stock_quantity,
                    deposit_id,
                    material_id,
                    tenant_id
                )
                select
                    m.buy_unit,
                    null as cost_per_item,
                    null as cost_price,
                    false as inactive,
                    m.request_unit,
                    0 as stock_available,
                    0 as stock_quantity,
                    :depositId,
                    m.id_material,
                    :tenantId
                from material m
                WHERE m.is_generic = false
                    AND m.tenant_id = :tenantId
                    AND inactive = false
            """
    )
    void insertMaterials(Long depositId, UUID tenantId);

}

