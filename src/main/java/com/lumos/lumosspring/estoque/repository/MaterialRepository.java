package com.lumos.lumosspring.estoque.repository;

import com.lumos.lumosspring.estoque.model.Material;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialRepository extends JpaRepository<Material, Long> {
}
