package com.lumos.lumosspring.stock.repository;

import com.lumos.lumosspring.stock.entities.Deposit;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepositRepository extends CrudRepository<Deposit, Long> {
    boolean existsByDepositName(String name);

    List<Deposit> findAllByOrderByIdDeposit();
}
