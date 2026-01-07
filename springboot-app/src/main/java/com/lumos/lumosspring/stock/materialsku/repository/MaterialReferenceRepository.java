package com.lumos.lumosspring.stock.materialsku.repository;

import com.lumos.lumosspring.stock.materialsku.model.Material;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface MaterialReferenceRepository extends CrudRepository<Material, Long> {
    @Query("""
        SELECT *
        FROM material
        WHERE (inactive IS NULL OR inactive = false)
          AND name_for_import IS NOT NULL
        """)
    List<Material> findAllForImportPreMeasurement();

    @Query("select id_material from material where material_name = :name")
    Long findBaseMaterialId(String name);

    Material findAllByBarcodeAndTenantId(String barcode, UUID tenantId);
}

