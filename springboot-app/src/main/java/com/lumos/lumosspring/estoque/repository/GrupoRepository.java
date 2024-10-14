package com.lumos.lumosspring.estoque.repository;

import com.lumos.lumosspring.estoque.model.Grupo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GrupoRepository extends JpaRepository<Grupo, Long> {
}
