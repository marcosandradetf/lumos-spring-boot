package com.lumos.lumosspring.contrato.repository;

import com.lumos.lumosspring.contrato.entities.ContratoItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContratoItensRepository extends JpaRepository<ContratoItem, Long> {
}
