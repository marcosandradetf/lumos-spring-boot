package com.lumos.lumosspring.team.repository;

import com.lumos.lumosspring.team.entities.Team;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamRepository extends CrudRepository<Team, Long> {
    @Query("SELECT id_team from team where driver_id = :userId or electrician_id = :userId")
    Optional<Long> getCurrentTeamId(UUID userId);

    Optional<Team> findByDriverId(UUID driverId);

    Optional<Team> findByElectricianId(UUID electricianId);

    Optional<Team> findByTeamName(String teamName);

}
