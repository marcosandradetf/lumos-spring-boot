package com.lumos.lumosspring.stock.repository;

import com.lumos.lumosspring.execution.dto.MaterialInStockDTO;
import com.lumos.lumosspring.stock.controller.dto.MaterialResponse;
import com.lumos.lumosspring.stock.entities.MaterialStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MaterialStockRepository extends CrudRepository<MaterialStock, Long> {
    @Query("""
        SELECT CASE WHEN COUNT(*) > 0 THEN TRUE ELSE FALSE END
        FROM material_stock ms
        JOIN material m ON ms.material_id = m.id_material
        WHERE m.material_name = :materialName
          AND ms.deposit_id = :idDeposit
          AND m.material_brand = :materialBrand
    """)
    boolean existsMaterial(@Param("materialName") String materialName,
                           @Param("idDeposit") Long idDeposit,
                           @Param("materialBrand") String materialBrand);

    @Query("""
        SELECT m.material_name
        FROM material_stock ms
        JOIN material m ON ms.material_id = m.id_material
        WHERE ms.material_id_stock = :id
    """)
    String GetNameById(@Param("id") Long id);


    @Query("""
        SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END
        FROM material m
        WHERE m.id_material_type = :id
    """)
    Optional<Integer> existsType(@Param("id") Long id);

    @Query("""
        SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END
        FROM material_stock ms
        WHERE ms.deposit_id = :id
    """)
    Optional<Integer> existsDeposit(@Param("id") Long id);


}

