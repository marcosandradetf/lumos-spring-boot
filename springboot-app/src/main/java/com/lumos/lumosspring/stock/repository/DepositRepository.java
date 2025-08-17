package com.lumos.lumosspring.stock.repository;

import com.lumos.lumosspring.stock.entities.Deposit;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepositRepository extends CrudRepository<Deposit, Long> {
    boolean existsByDepositName(String name);

    @Query("SELECT * FROM deposit WHERE is_truck = false ORDER BY deposit_name")
    List<Deposit> findAllByOrderByIdDeposit();

    @Query("SELECT * FROM deposit WHERE is_truck = true ORDER BY deposit_name")
    List<Deposit> findAllTruckDeposit();

    @Query("SELECT deposit_id_deposit from team where id_team = :teamId")
    Long getDepositIdByTeamId(long teamId);
}
