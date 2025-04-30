package com.lumos.lumosspring.stock.repository;

import com.lumos.lumosspring.stock.entities.Deposit;
import com.lumos.lumosspring.stock.entities.Type;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepositRepository extends JpaRepository<Deposit, Long> {
    boolean existsByDepositName(String name);

    List<Deposit> findAllByOrderByIdDeposit();
}
