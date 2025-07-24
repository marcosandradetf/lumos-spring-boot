package com.lumos.lumosspring.stock.repository;

import com.lumos.lumosspring.stock.entities.Material;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialRepository extends CrudRepository<Material, Long> {
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

}

