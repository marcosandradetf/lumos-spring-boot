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
    boolean existsByMaterialName(String materialName); // verificar duplicatas

    @Query("""
        SELECT m.*
        FROM material m
        JOIN material_type mt ON m.id_material_type = mt.id_type
        WHERE UPPER(mt.type_name) IN ('LED', 'CINTA', 'PARAFUSO', 'CONECTOR', 'POSTE', 'REFLETOR')
           OR UPPER(mt.type_name) LIKE 'BRA%'
        ORDER BY mt.type_name,
                 CAST(REGEXP_REPLACE(m.material_power, '[^0-9]+', '', 'g') AS INTEGER),
                 CAST(REGEXP_REPLACE(m.material_length, '[^0-9]+', '', 'g') AS INTEGER)
        """)
    List<Material> getMaterialsForInstallation();

    @Query("""
        SELECT *
        FROM material m
        JOIN material_type mt ON m.id_material_type = mt.id_type
        WHERE UPPER(mt.type_name) IN ('LED', 'POSTE', 'RELÉ', 'CABO')
           OR UPPER(mt.type_name) LIKE 'BRA%'
        """)
    List<Material> findAllMaterialsExcludingScrewStrapAndConnector();

    @Query("""
        SELECT *
        FROM material m
        JOIN material_type mt ON m.id_material_type = mt.id_type
        WHERE UPPER(mt.type_name) IN ('PARAFUSO', 'CONECTOR', 'CINTA')
          AND m.id_material = (
              SELECT MIN(m2.id_material)
              FROM material m2
              JOIN material_type mt2 ON m2.id_material_type = mt2.id_type
              WHERE UPPER(mt2.type_name) = UPPER(mt.type_name)
          )
        """)
    List<Material> findOneScrewStrapAndConnector();

    @Query("""
        SELECT *
        FROM material
        WHERE (inactive IS NULL OR inactive = false)
          AND name_for_import IS NOT NULL
        """)
    List<Material> findAllForImportPreMeasurement();

    @Query("""
        SELECT CASE\s
            WHEN ms.stock_available >= :materialQuantity THEN null\s
            ELSE m.material_name\s
        END
        FROM material m
        JOIN material_stock ms ON ms.material_id = m.id_material
        WHERE ms.deposit_id = :depositId AND m.id_material = :materialId
   \s""")
    String hasStockAvailable(
            @Param("materialId") Long materialId,
            @Param("depositId") Long depositId,
            @Param("materialQuantity") BigDecimal materialQuantity
    );

    @Query("select id_material from material where material_name = :name")
    Long findBaseMaterialId(String name);

    Material findAllByBarcodeAndTenantId(String barcode, UUID tenantId);
}

