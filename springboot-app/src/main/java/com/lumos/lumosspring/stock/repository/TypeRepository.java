package com.lumos.lumosspring.stock.repository;

import com.lumos.lumosspring.stock.entities.Type;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TypeRepository extends JpaRepository<Type, Long> {
    boolean existsByTypeName(String name);

    List<Type> findAllByOrderByIdTypeAsc();

    @Query("select 1 from Type t where t.group.idGroup = :groupId")
    Optional<Integer> existsGroup(@Param("groupId") Long groupId);
}
