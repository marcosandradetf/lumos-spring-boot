package com.lumos.lumosspring.stock.repository;

import com.lumos.lumosspring.stock.entities.MaterialType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TypeRepository extends JpaRepository<MaterialType, Long> {
    boolean existsByTypeName(String name);

    List<MaterialType> findAllByOrderByIdTypeAsc();

    @Query("select 1 from MaterialType t where t.materialGroup.idGroup = :groupId")
    Optional<Integer> existsGroup(@Param("groupId") Long groupId);
}
