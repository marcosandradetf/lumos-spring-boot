package com.lumos.lumosspring.stock.materialsku.repository;

import com.lumos.lumosspring.stock.materialsku.model.MaterialType;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TypeRepository extends CrudRepository<MaterialType, Long> {
    record TypeUnitResponse(String code, String description, Boolean truckStockControl, Boolean buyUnit) {}
    record TypeSubtypeResponse(Long typeId, String typeName, Long subtypeId, String subtypeName) {}

    boolean existsByTypeName(String name);

    List<MaterialType> findAllByOrderByIdTypeAsc();

    @Query("""
        SELECT mt.id_type as type_id, mt.type_name, mst.subtype_id, mst.subtype_name
        FROM material_type mt
        LEFT JOIN material_subtype mst on mst.type_id = mt.id_type
        ORDER BY mt.type_name
    """)
    List<TypeSubtypeResponse> findAllTypeSubtype();

    @Query("""
        SELECT bu.code, bu.description, bu.truck_stock_control, true as buy_unit
        FROM material_type_buy_unit mtbu
        JOIN unit bu on bu.unit_id = mtbu.unit_id
        WHERE mtbu.material_type_id = :typeId
        UNION ALL
        SELECT ru.code, ru.description, ru.truck_stock_control, false as buy_unit
        FROM material_type_request_unit mtru
        JOIN unit ru on ru.unit_id = mtru.unit_id
        WHERE mtru.material_type_id = :typeId
        order by buy_unit, code
    """)
    List<TypeUnitResponse> findUnitsByTypeId(Long typeId);

    @Query("SELECT 1 FROM material_type WHERE id_group = :groupId LIMIT 1")
    Optional<Integer> existsGroup(@Param("groupId") Long groupId);
}
