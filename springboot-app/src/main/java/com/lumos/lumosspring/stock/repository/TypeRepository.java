package com.lumos.lumosspring.stock.repository;

import com.lumos.lumosspring.stock.entities.MaterialType;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TypeRepository extends CrudRepository<MaterialType, Long> {
    boolean existsByTypeName(String name);

    List<MaterialType> findAllByOrderByIdTypeAsc();

    @Query("SELECT 1 FROM material_type WHERE id_group = :groupId LIMIT 1")
    Optional<Integer> existsGroup(@Param("groupId") Long groupId);
}
