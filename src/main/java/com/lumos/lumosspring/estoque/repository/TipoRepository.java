package com.lumos.lumosspring.estoque.repository;

import com.lumos.lumosspring.estoque.model.Tipo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoRepository extends JpaRepository<Tipo, Long> {
}
