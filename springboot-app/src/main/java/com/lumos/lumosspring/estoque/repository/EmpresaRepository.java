package com.lumos.lumosspring.estoque.repository;

import com.lumos.lumosspring.estoque.model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
}
