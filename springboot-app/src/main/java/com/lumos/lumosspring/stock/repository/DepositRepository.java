package com.lumos.lumosspring.stock.repository;

import com.lumos.lumosspring.stock.entities.Deposit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepositRepository extends JpaRepository<Deposit, Long> {
}
