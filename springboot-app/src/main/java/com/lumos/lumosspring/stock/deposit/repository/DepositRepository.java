package com.lumos.lumosspring.stock.deposit.repository;

import com.lumos.lumosspring.stock.deposit.dto.DepositResponse;
import com.lumos.lumosspring.stock.deposit.model.Deposit;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DepositRepository extends CrudRepository<Deposit, Long> {
    boolean existsByDepositName(String name);

    @Query("""
        SELECT d.id_deposit, d.deposit_name, d.deposit_address,
               d.deposit_district, d.deposit_city, d.deposit_state, r.region_name as deposit_region,
               d.deposit_phone, d.is_truck, t.team_name, t.plate_vehicle
        FROM deposit d
        LEFT JOIN team t ON t.deposit_id_deposit = d.id_deposit
        JOIN region r on r.region_id = d.region_id
        WHERE d.tenant_id = :tenantId
        ORDER BY d.id_deposit
   """)
    List<DepositResponse> findAllByOrderByIdDeposit(UUID tenantId);

    @Query("SELECT deposit_id_deposit from team where id_team = :teamId")
    Long getDepositIdByTeamId(long teamId);

    @Query("""
        select true
        from team
        where deposit_id_deposit = :id
    """)
    Boolean hasTeam(@Param("id") Long id);
}
