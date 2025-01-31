package com.lumos.lumosspring.stock.repository;

import com.lumos.lumosspring.stock.entities.Material;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    boolean existsByMaterialName(String materialName); // verificar duplicatas
    @Query("SELECT COUNT(m) > 0 FROM Material m WHERE m.materialName = :materialName AND m.deposit.idDeposit = :idDeposit AND m.materialBrand = :materialBrand")
    boolean existsMaterial(@Param("materialName") String materialName,
                           @Param("idDeposit") Long idDeposit,
                           @Param("materialBrand") String materialBrand);

    @Query(value = "SELECT * FROM tb_materials m " +
            "INNER JOIN tb_types t ON t.id_type = m.id_material_type " +
            "WHERE unaccent(LOWER(m.material_name)) LIKE unaccent(concat('%', :name, '%')) " +
            "OR unaccent(LOWER(t.type_name)) LIKE unaccent(concat('%', :name, '%'))",
            nativeQuery = true)
    Page<Material> findByMaterialNameOrTypeIgnoreAccent(Pageable pageable, String name);

    @Query("SELECT m FROM Material m WHERE m.deposit.idDeposit IN :depositIds")
    Page<Material> findByDeposit(Pageable pageable, @Param("depositIds") List<Long> depositIds);

    @Query("SELECT m.materialName from Material m where m.idMaterial = :id")
    String GetNameById(Long id);

    @Query("select m from Material m order by m.idMaterial")
    Page<Material> findAllOrderByIdMaterial(Pageable pageable);

    @Query("SELECT 1 FROM Material m WHERE m.materialType.idType = :id")
    Optional<Integer> existsType(@Param("id") Long id);

    @Query("SELECT 1 FROM Material m WHERE m.deposit.idDeposit = :id")
    Optional<Integer> existsDeposit(@Param("id") Long id);

    @Query("SELECT m FROM Material m WHERE m.deposit.idDeposit = :depositIds")
    List<Material> getByDeposit(@Param("depositIds") Long depositId);

    List<Material> findAllByOrderByMaterialName();
}

