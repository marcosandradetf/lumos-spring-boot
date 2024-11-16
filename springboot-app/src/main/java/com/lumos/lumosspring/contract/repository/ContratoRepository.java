package com.lumos.lumosspring.contract.repository;

import com.lumos.lumosspring.contract.entities.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContratoRepository extends JpaRepository<Contract, Long> {
}
