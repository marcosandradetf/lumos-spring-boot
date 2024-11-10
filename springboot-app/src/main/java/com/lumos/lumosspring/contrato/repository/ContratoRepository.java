package com.lumos.lumosspring.contrato.repository;

import com.lumos.lumosspring.contrato.entities.Contrato;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContratoRepository extends JpaRepository<Contrato, Long> {
}
