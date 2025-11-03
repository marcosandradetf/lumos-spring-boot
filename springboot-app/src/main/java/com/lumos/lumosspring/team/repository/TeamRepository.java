package com.lumos.lumosspring.team.repository;

import com.lumos.lumosspring.team.dto.MemberTeamResponse;
import com.lumos.lumosspring.team.dto.TeamResponseForConfirmation;
import com.lumos.lumosspring.team.model.Team;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

import java.util.Optional;
import java.util.UUID;

@Repository
    public interface TeamRepository extends CrudRepository<Team, Long> {
    @Query("SELECT team_id from app_user where user_id = :userId")
    Optional<Long> getCurrentTeamId(UUID userId);

    @Query("""
        select t.id_team as team_id,
               d.deposit_name as deposit_name,
               t.team_name as team_name,
               t.plate_vehicle as plate_vehicle
        from team t
        join deposit d on d.id_deposit = t.deposit_id_deposit
        WHERE t.tenant_id = :tenantId
   \s""")
    List<TeamResponseForConfirmation> getTeams(UUID tenantId);

    @Query("Select name || ' ' || last_name as member_name, user_id from app_user where team_id = :teamId")
    List<MemberTeamResponse> getMembers(long teamId);

}
