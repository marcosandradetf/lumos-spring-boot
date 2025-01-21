package com.lumos.lumosspring.execution.repository;

import com.lumos.lumosspring.execution.entities.Street;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StreetRepository extends JpaRepository<Street, Long> {
}
