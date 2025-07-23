package com.lumos.lumosspring.stock.repository;

import com.lumos.lumosspring.stock.entities.Deposit;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface DepositRepository extends CrudRepository<Deposit, Long> {
    boolean existsByDepositName(String name);

    List<Deposit> findAllByOrderByIdDeposit();
}
