package com.lumos.lumosspring.stock.repository;

import com.lumos.lumosspring.stock.entities.Material;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    boolean existsByMaterialName(String materialName); // verificar duplicatas

//    @Query("SELECT m FROM Material m " +
//            "INNER JOIN Type t ON t.idType = m.materialType.idType " +
//            "WHERE unaccent(m.materialName) LIKE unaccent(:name) " +
//            "OR unaccent(t.typeName) LIKE unaccent(:name)")
//    Page<Material> findByMaterialNameOrTypeIgnoreAccent(Pageable pageable, String name);
//

    @Query(value = "SELECT * FROM tb_materials m " +
            "INNER JOIN tb_types t ON t.id_type = m.id_material_type " +
            "WHERE unaccent(LOWER(m.material_name)) LIKE unaccent(concat('%', :name, '%')) " +
            "OR unaccent(LOWER(t.type_name)) LIKE unaccent(concat('%', :name, '%'))",
            nativeQuery = true)
    Page<Material> findByMaterialNameOrTypeIgnoreAccent(Pageable pageable, String name);

//
//    @Query("SELECT m FROM Material m WHERE unaccent(m.materialName) LIKE unaccent(:name) OR unaccent(m.materialType.typeName) LIKE unaccent(:name)")
//    Page<Material> findByMaterialNameOrTypeIgnoreAccent(Pageable pageable, String name);
}

