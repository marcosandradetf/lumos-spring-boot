package com.lumos.lumosspring.stock.materialstock.repository;

import com.lumos.lumosspring.stock.materialsku.dto.MaterialResponse;
import com.lumos.lumosspring.stock.materialstock.dto.MaterialInStockDTO;
import com.lumos.lumosspring.stock.materialstock.model.MaterialStock;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaterialStockViewRepository extends CrudRepository<MaterialStock, Long> {
    @Query(
        """
                SELECT
                    ms.material_id_stock as material_stock_id,
                    m.id_material as material_id,
                    m.material_name,
                    m.barcode,
                    m.buy_unit,
                    m.request_unit,
                    ms.stock_quantity,
                    d.deposit_name
                FROM material_stock ms
                JOIN material m ON ms.material_id = m.id_material
                JOIN deposit d ON ms.deposit_id = d.id_deposit
                WHERE ms.deposit_id = :depositId
                ORDER BY m.material_name
                LIMIT :size OFFSET :offset
        """
    )
    List<MaterialResponse> getAllMaterialsWithPagination(Integer size, Integer offset, Long depositId);

    @Query(
            """
                SELECT
                    ms.material_id_stock as material_stock_id,
                    m.id_material as material_id,
                    m.material_name,
                    m.barcode,
                    m.buy_unit,
                    m.request_unit,
                    ms.stock_quantity,
                    d.deposit_name
                FROM material_stock ms
                JOIN material m ON ms.material_id = m.id_material
                JOIN deposit d ON ms.deposit_id = d.id_deposit
                WHERE ms.deposit_id = :depositId
                  AND (
                    m.material_name_unaccent LIKE :likeName
                    OR m.barcode LIKE :likeName
                  )
                ORDER BY m.material_name
                LIMIT :size OFFSET :offset
            """
    )
    List<MaterialResponse> getMaterialsBySearchWithPagination(Integer size, Integer offset, Long depositId, String likeName);

    Integer countMaterialStockByDepositId(Long depositId);

    @Query("""
        SELECT COUNT(*)
        FROM material_stock ms
        JOIN material m ON ms.material_id = m.id_material
        WHERE deposit_id = :depositId
            AND (
                    m.material_name_unaccent LIKE :likeName
                    OR LOWER(m.barcode) LIKE :likeName
            )
    """)
    Integer countMaterialStockByDepositIdAndMaterialName(Long depositId, String materialName, String likeName);

    @Query("""
    SELECT CASE WHEN EXISTS (
        SELECT 1
        FROM material_stock ms
        WHERE ms.deposit_id = :id
          AND ms.stock_quantity > 0
    ) THEN 1 ELSE 0 END
    """)
    Optional<Integer> existsDeposit(Long id);

    @Query("""
        SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END
        FROM material m
        WHERE m.id_material_type = :id
    """)
    Optional<Integer> existsType(@Param("id") Long id);

    @Query("""
        select
            ms.material_id_stock as material_stock_id,
            ms.material_id,
            m.material_name,
            m.barcode,
            m.buy_unit,
            m.request_unit,
            ms.stock_quantity,
            d.deposit_name
        from material_stock ms
        join material m on m.id_material = ms.material_id
        join deposit d on d.id_deposit = ms.deposit_id
    """)
    Optional<MaterialResponse> findByBarcodeAndDepositId(@NotNull String barcode, long depositId);
// find by linking - deprecated
//    @Query(
//            """
//                 SELECT
//                   ms.material_id_stock AS materialIdStock,
//                   m.id_material AS materialId,
//                   m.material_name AS materialName,
//                   m.material_power AS materialPower,
//                   m.material_length AS materialLength,
//                   mt.type_name AS typeName,
//                   d.deposit_name AS depositName,
//                   ms.stock_available AS stockAvailable,
//                   ms.request_unit AS requestUnit,
//                   d.is_truck AS isTruck,
//                   t.plate_vehicle as plateVehicle
//                 FROM material_stock ms
//                   JOIN material m ON ms.material_id = m.id_material
//                   JOIN material_type mt ON m.id_material_type = mt.id_type
//                   JOIN deposit d ON ms.deposit_id = d.id_deposit
//                   LEFT JOIN team t on t.deposit_id_deposit = d.id_deposit
//                 WHERE LOWER(mt.type_name) = :type
//                   AND ms.inactive = false
//                   AND (
//                     t.id_team = :teamId
//                     OR d.is_truck = false
//                   )
//                   AND ms.tenant_id = :tenantId
//                 ORDER BY  (d.is_truck::int) DESC, d.deposit_name;
//             """
//    )
//    List<MaterialInStockDTO> findAllByType(String type, Long teamId);

//    @Query(
//            """
//                SELECT
//                  ms.material_id_stock AS materialIdStock,
//                  m.id_material AS materialId,
//                  m.material_name AS materialName,
//                  m.material_power AS materialPower,
//                  m.material_length AS materialLength,
//                  mt.type_name AS typeName,
//                  d.deposit_name AS depositName,
//                  ms.stock_available AS stockAvailable,
//                  ms.request_unit AS requestUnit,
//                  d.is_truck AS isTruck,
//                  t.plate_vehicle as plateVehicle
//                FROM material_stock ms
//                  JOIN material m ON ms.material_id = m.id_material
//                  JOIN material_type mt ON m.id_material_type = mt.id_type
//                  JOIN deposit d ON ms.deposit_id = d.id_deposit
//                  LEFT JOIN team t on t.deposit_id_deposit = d.id_deposit
//                WHERE LOWER(mt.type_name) = :type
//                  AND (
//                    LOWER(m.material_power) = :linking
//                    OR LOWER(m.material_length) = :linking
//                  )
//                  AND ms.inactive = false
//                  AND (
//                    t.id_team = :teamId
//                    OR d.is_truck = false
//                  )
//                  AND ms.tenant_id = :tenantId
//                ORDER BY  (d.is_truck::int) DESC, d.deposit_name;
//            """
//    )
//    List<MaterialInStockDTO> findAllByLinkingAndType(String linking, String type, Long teamId);

    @Query(
        """
            SELECT
                ms.material_id_stock AS material_stock_id,
                m.id_material AS material_id,
                m.material_name,
                d.deposit_name,
                ms.stock_available,
                m.request_unit,
                d.is_truck,
                t.plate_vehicle,
                mcri.contract_reference_item_id
            FROM material_stock ms
            JOIN material m ON ms.material_id = m.id_material
            JOIN material_contract_reference_item mcri on mcri.material_id = m.id_material
            JOIN deposit d ON ms.deposit_id = d.id_deposit
            LEFT JOIN team t on t.deposit_id_deposit = d.id_deposit
            WHERE m.inactive = false
                AND mcri.contract_reference_item_id = :contractReferenceItemId
                AND (t.id_team = :teamId or d.is_truck = false)
            ORDER BY  (d.is_truck::int) DESC, d.deposit_name;
        """
    )
    List<MaterialInStockDTO> findMaterialsByContractReference(Long contractReferenceItemId, Long teamId);
}

