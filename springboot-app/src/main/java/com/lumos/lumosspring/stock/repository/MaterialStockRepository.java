package com.lumos.lumosspring.stock.repository;

import com.lumos.lumosspring.execution.dto.MaterialInStockDTO;
import com.lumos.lumosspring.stock.controller.dto.MaterialResponse;
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

    @Query("SELECT ms.material.materialName from MaterialStock ms where ms.material.idMaterial = :id")
    String GetNameById(Long id);

    @Query(value = "select new com.lumos.lumosspring.stock.controller.dto.MaterialResponse(" +
            "ms.materialIdStock, ms.material.materialName, ms.material.materialBrand," +
            "ms.material.materialPower, ms.material.materialAmps, ms.material.materialLength," +
            "ms.buyUnit, ms.requestUnit, ms.stockQuantity, ms.inactive, ms.material.materialType.typeName," +
            "ms.material.materialType.materialGroup.groupName, ms.deposit.depositName, ms.company.fantasyName) " +
            "from MaterialStock ms order by ms.materialIdStock",
            countQuery = "select count(ms.materialIdStock) from MaterialStock ms"
    )
    Page<MaterialResponse> findAllMaterialsStock(Pageable pageable);

    @Query(value = "select new com.lumos.lumosspring.stock.controller.dto.MaterialResponse(" +
            "ms.materialIdStock, ms.material.materialName, ms.material.materialBrand," +
            "ms.material.materialPower, ms.material.materialAmps, ms.material.materialLength," +
            "ms.buyUnit, ms.requestUnit, ms.stockQuantity, ms.inactive, ms.material.materialType.typeName," +
            "ms.material.materialType.materialGroup.groupName, ms.deposit.depositName, ms.company.fantasyName) " +
            "from MaterialStock ms where ms.deposit.idDeposit = :depositId order by ms.materialIdStock",
            countQuery = "select count(ms.materialIdStock) from MaterialStock ms where ms.deposit.idDeposit = :depositId"
    )
    Page<MaterialResponse> findAllMaterialsStockByDeposit(Pageable pageable, Long depositId);


    @Query("SELECT 1 FROM MaterialStock ms WHERE ms.material.materialType.idType = :id")
    Optional<Integer> existsType(@Param("id") Long id);

    @Query("SELECT 1 FROM MaterialStock ms WHERE ms.deposit.idDeposit = :id")
    Optional<Integer> existsDeposit(@Param("id") Long id);

    @Query(value = "select new com.lumos.lumosspring.stock.controller.dto.MaterialResponse(" +
            "ms.materialIdStock, ms.material.materialName, ms.material.materialBrand," +
            "ms.material.materialPower, ms.material.materialAmps, ms.material.materialLength," +
            "ms.buyUnit, ms.requestUnit, ms.stockQuantity, ms.inactive, ms.material.materialType.typeName," +
            "ms.material.materialType.materialGroup.groupName, ms.deposit.depositName, ms.company.fantasyName) " +
            "FROM MaterialStock ms " +
            "WHERE LOWER(ms.material.materialName) LIKE concat('%', :name, '%') OR " +
            "LOWER(ms.material.materialType.typeName) LIKE concat('%', :name, '%') OR " +
            "LOWER(ms.material.materialPower) LIKE concat('%', :name, '%') OR " +
            "LOWER(ms.material.materialLength) LIKE concat('%', :name, '%') order by ms.materialIdStock",
            countQuery = "select count(ms.materialIdStock) from MaterialStock ms " +
                    "WHERE LOWER(ms.material.materialName) LIKE concat('%', :name, '%') OR " +
                    "LOWER(ms.material.materialType.typeName) LIKE concat('%', :name, '%') OR " +
                    "LOWER(ms.material.materialPower) LIKE concat('%', :name, '%') OR " +
                    "LOWER(ms.material.materialLength) LIKE concat('%', :name, '%')"
    )
    Page<MaterialResponse> searchMaterial(Pageable pageable, String name);

    @Query(value = "select new com.lumos.lumosspring.stock.controller.dto.MaterialResponse(" +
            "ms.materialIdStock, ms.material.materialName, ms.material.materialBrand," +
            "ms.material.materialPower, ms.material.materialAmps, ms.material.materialLength," +
            "ms.buyUnit, ms.requestUnit, ms.stockQuantity, ms.inactive, ms.material.materialType.typeName," +
            "ms.material.materialType.materialGroup.groupName, ms.deposit.depositName, ms.company.fantasyName) " +
            "FROM MaterialStock ms " +
            "WHERE ms.deposit.idDeposit = :depositId AND " +
            "(LOWER(ms.material.materialName) LIKE concat('%', :name, '%') OR " +
            "LOWER(ms.material.materialType.typeName) LIKE concat('%', :name, '%') OR " +
            "LOWER(ms.material.materialPower) LIKE concat('%', :name, '%') OR " +
            "LOWER(ms.material.materialLength) LIKE concat('%', :name, '%')) order by ms.materialIdStock",
            countQuery = "select count(ms.materialIdStock) from MaterialStock ms " +
                    "WHERE ms.deposit.idDeposit = :depositId AND " +
                    "(LOWER(ms.material.materialName) LIKE concat('%', :name, '%') OR " +
                    "LOWER(ms.material.materialType.typeName) LIKE concat('%', :name, '%') OR " +
                    "LOWER(ms.material.materialPower) LIKE concat('%', :name, '%') OR " +
                    "LOWER(ms.material.materialLength) LIKE concat('%', :name, '%'))"
    )
    Page<MaterialResponse> searchMaterialWithDeposit(Pageable pageable, String name, long depositId);

    @Query("SELECT new com.lumos.lumosspring.execution.dto.MaterialInStockDTO(" +
            "ms.materialIdStock, ms.material.materialName, ms.material.materialPower, ms.material.materialLength," +
            "ms.material.materialType.typeName, ms.deposit.depositName, ms.stockAvailable, ms.requestUnit) " +
            "FROM MaterialStock ms " +
            "WHERE LOWER(ms.material.materialType.typeName) = :type " +
            "AND ms.inactive = false " +
            "AND (LOWER(ms.deposit.depositName) = LOWER(:depositName) " +
            "     OR LOWER(ms.deposit.depositName) NOT LIKE '%caminhão%') order by  ms.deposit.depositName")
    List<MaterialInStockDTO> findAllByType(@Param("type") String type, @Param("depositName") String depositName);

    @Query("SELECT new com.lumos.lumosspring.execution.dto.MaterialInStockDTO(" +
            "ms.materialIdStock, ms.material.materialName, ms.material.materialPower, ms.material.materialLength," +
            "ms.material.materialType.typeName, ms.deposit.depositName, ms.stockAvailable, ms.requestUnit) " +
            "FROM MaterialStock ms " +
            "WHERE LOWER(ms.material.materialType.typeName) = :type " +
            "AND (LOWER(ms.material.materialPower) = :linking " +
            "   OR LOWER(ms.material.materialLength) = :linking) " +
            "AND ms.inactive = false " +
            "AND (LOWER(ms.deposit.depositName) = LOWER(:depositName) " +
            "     OR LOWER(ms.deposit.depositName) NOT LIKE '%caminhão%') order by ms.deposit.depositName")
    List<MaterialInStockDTO> findAllByLinkingAndType(@Param("linking") String linking, @Param("type") String type, @Param("depositName") String depositName);


}

