package com.lumos.lumosspring.team.repository;

import com.lumos.lumosspring.team.entities.Team;
import com.lumos.lumosspring.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByDriver(AppUser driver);

    Optional<Team> findByElectrician(AppUser electrician);

    Optional<Team> findByTeamName(String teamName);

}
