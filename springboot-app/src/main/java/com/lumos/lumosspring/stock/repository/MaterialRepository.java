package com.lumos.lumosspring.stock.repository;

import com.lumos.lumosspring.stock.entities.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    boolean existsByMaterialName(String materialName); // verificar duplicatas

    @Query("SELECT m FROM Material m " +
            "INNER JOIN m.materialType mt " +
            "WHERE UPPER(mt.typeName) IN ('LED', 'CINTA', 'PARAFUSO', 'CONECTOR', 'POSTE') " +
            "OR UPPER(mt.typeName) LIKE('BRA%')" +
            "ORDER BY mt.typeName, " +
            "CAST(FUNCTION('REGEXP_REPLACE', m.materialPower, '[^0-9]+', '', 'g') AS integer), " +
            "CAST(FUNCTION('REGEXP_REPLACE', m.materialLength, '[^0-9]+', '', 'g') AS integer)")
    List<Material> getMaterialsForInstallation();

    Optional<Material> findFirstByMaterialType_TypeNameOrMaterialType_TypeName(
            String typeName, String typeName2);

    @Query("SELECT m FROM Material m WHERE UPPER(m.materialType.typeName) IN ('LED', 'CINTA', 'POSTE') OR UPPER(m.materialType.typeName) LIKE('BRA%')")
    List<Material> findAllMaterialsExcludingScrewAndConnector();

    @Query("SELECT m FROM Material m  WHERE UPPER(m.materialType.typeName)  IN ('PARAFUSO', 'CONECTOR') AND m.idMaterial = " +
            "(SELECT MIN(m2.idMaterial) FROM Material m2  WHERE UPPER(m2.materialType.typeName) = UPPER(m.materialType.typeName))")
    List<Material> findOneScrewAndConnector();

}

