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
    record typeSubtypeResponse (Long typeId, String typeName, Long subtypeId, String subtypeName) {}

    boolean existsByTypeName(String name);

    List<MaterialType> findAllByOrderByIdTypeAsc();

    @Query("""
        SELECT mst.type_id, mt.type_name, mst.subtype_id, mst.subtype_name
        FROM material_type mt
        JOIN material_subtype mst on mst.type_id = mt.id_type
        ORDER BY mt.type_name
    """)
    List<typeSubtypeResponse> findAllTypeSubtype();

    @Query("SELECT 1 FROM material_type WHERE id_group = :groupId LIMIT 1")
    Optional<Integer> existsGroup(@Param("groupId") Long groupId);
}
