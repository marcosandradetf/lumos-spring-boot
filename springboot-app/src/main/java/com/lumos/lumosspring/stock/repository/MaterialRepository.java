package com.lumos.lumosspring.stock.repository;

import com.lumos.lumosspring.stock.entities.Material;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    boolean existsByMaterialName(String materialName); // verificar duplicatas
}
