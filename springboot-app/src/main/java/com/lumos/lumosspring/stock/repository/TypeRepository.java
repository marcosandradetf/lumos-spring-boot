package com.lumos.lumosspring.stock.repository;

import com.lumos.lumosspring.stock.entities.Type;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TypeRepository extends JpaRepository<Type, Long> {
    boolean existsByTypeName(String name);

    List<Type> findAllByOrderByIdTypeAsc();
}
