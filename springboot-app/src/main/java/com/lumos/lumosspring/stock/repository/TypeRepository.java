package com.lumos.lumosspring.stock.repository;

import com.lumos.lumosspring.stock.entities.Type;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TypeRepository extends JpaRepository<Type, Long> {
}
