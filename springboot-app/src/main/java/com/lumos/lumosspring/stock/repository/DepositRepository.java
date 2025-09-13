package com.lumos.lumosspring.stock.repository;

import com.lumos.lumosspring.dto.stock.DepositResponse;
import com.lumos.lumosspring.stock.entities.Deposit;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepositRepository extends CrudRepository<Deposit, Long> {
    boolean existsByDepositName(String name);

    @Query("""
        SELECT d.id_deposit, d.deposit_name, c.fantasy_name as company_name, d.deposit_address,\s
               d.deposit_district, d.deposit_city, d.deposit_state, r.region_name as deposit_region,
               d.deposit_phone, d.is_truck, t.team_name, t.plate_vehicle
        FROM deposit d
        LEFT JOIN team t ON t.deposit_id_deposit = d.id_deposit
        join company c on c.id_company = d.company_id
        join region r on r.region_id = d.region_id
        ORDER BY d.id_deposit
   \s""")
    List<DepositResponse> findAllByOrderByIdDeposit();

    @Query("SELECT deposit_id_deposit from team where id_team = :teamId")
    Long getDepositIdByTeamId(long teamId);

    @Query("""
        select true
        from team
        where deposit_id_deposit = :id
    """)
    Boolean hasTeam(@Param("id") Long id);
}
