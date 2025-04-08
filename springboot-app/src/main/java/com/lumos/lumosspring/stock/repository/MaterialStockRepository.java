package com.lumos.lumosspring.stock.repository;

import com.lumos.lumosspring.stock.entities.Material;
import com.lumos.lumosspring.stock.entities.MaterialStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MaterialStockRepository extends JpaRepository<MaterialStock, Long> {
    @Query("SELECT COUNT(ms) > 0 FROM MaterialStock ms WHERE ms.material.materialName = :materialName AND ms.deposit.idDeposit = :idDeposit AND ms.material.materialBrand = :materialBrand")
    boolean existsMaterial(@Param("materialName") String materialName,
                           @Param("idDeposit") Long idDeposit,
                           @Param("materialBrand") String materialBrand);

    @Query("SELECT ms FROM MaterialStock ms WHERE ms.deposit.idDeposit IN :depositIds")
    Page<MaterialStock> findByDeposit(Pageable pageable, @Param("depositIds") List<Long> depositIds);

    @Query("SELECT ms.material.materialName from MaterialStock ms where ms.material.idMaterial = :id")
    String GetNameById(Long id);
    
    @Query("select ms from MaterialStock ms order by ms.materialIdStock")
    Page<MaterialStock> findAllOrderByIdMaterial(Pageable pageable);

    @Query("SELECT 1 FROM MaterialStock ms WHERE ms.material.materialType.idType = :id")
    Optional<Integer> existsType(@Param("id") Long id);

    @Query("SELECT 1 FROM MaterialStock ms WHERE ms.deposit.idDeposit = :id")
    Optional<Integer> existsDeposit(@Param("id") Long id);

    @Query("SELECT ms FROM MaterialStock ms WHERE ms.deposit.idDeposit = :depositId ORDER BY ms.material.materialType.typeName, ms.material.materialPower, ms.material.materialLength")
    List<MaterialStock> getByDeposit(@Param("depositId") Long depositId);

    @Query("SELECT ms FROM MaterialStock ms WHERE UPPER(ms.material.materialType.typeName) NOT IN ('RELE', 'CABO', 'RELÉ') ORDER BY ms.material.materialType.typeName, ms.material.materialPower, ms.material.materialLength")
    List<MaterialStock> findAllByOrderByMaterialName();

    @Query(value = "SELECT * FROM tb_material_stock ms " +
            "INNER JOIN tb_materials m on ms.material_id = m.id_material " +
            "INNER JOIN  tb_types t ON t.id_type = m.id_material_type " +
            "WHERE unaccent(LOWER(m.material_name)) LIKE unaccent(concat('%', :name, '%')) " +
            "OR unaccent(LOWER(t.type_name)) LIKE unaccent(concat('%', :name, '%'))",
            nativeQuery = true)
    Page<MaterialStock> findByMaterialNameOrTypeIgnoreAccent(Pageable pageable, String name);

    @Query("SELECT ms FROM MaterialStock ms WHERE ms.stockAvailable > 0 AND ms.material IN :materials and not ms.inactive")
    List<MaterialStock> findAvailableFiltered(@Param("materials") List<Material> materials);
}

