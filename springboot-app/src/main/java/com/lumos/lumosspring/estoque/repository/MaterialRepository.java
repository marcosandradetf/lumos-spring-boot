package com.lumos.lumosspring.estoque.repository;

import com.lumos.lumosspring.estoque.model.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    boolean existsByNomeMaterial(String nomeMaterial); // verificar duplicatas
}
