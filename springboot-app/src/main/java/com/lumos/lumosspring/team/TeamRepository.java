package com.lumos.lumosspring.team;

import com.lumos.lumosspring.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByDriver(User driver);
    Optional<Team> findByElectrician(User electrician);
    Optional<Team> findByTeamName(String teamName);
}
