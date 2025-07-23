package com.lumos.lumosspring.team.repository;

import com.lumos.lumosspring.team.entities.Team;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface TeamRepository extends CrudRepository<Team, Long> {
    Optional<Team> findByDriverId(UUID driverId);

    Optional<Team> findByElectricianId(UUID electricianId);

    Optional<Team> findByTeamName(String teamName);

}
