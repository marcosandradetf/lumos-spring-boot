package com.lumos.lumosspring.stock.materialstock.repository;

import com.lumos.lumosspring.stock.materialstock.model.MaterialStock;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
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
    SELECT CASE WHEN EXISTS (
        SELECT 1
        FROM material_stock ms
        WHERE ms.deposit_id = :id
          AND ms.stock_quantity > 0
    ) THEN 1 ELSE 0 END
    """)
    Optional<Integer> existsDeposit(@Param("id") Long id);

}

