package com.lumos.lumosspring.team.repository;

import com.lumos.lumosspring.team.entities.Team;
import com.lumos.lumosspring.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByDriver(User driver);

    Optional<Team> findByElectrician(User electrician);

    Optional<Team> findByTeamName(String teamName);

    @Query("""
                 SELECT t FROM Team t\s
                 WHERE t.driver.idUser = :userId\s
                    OR t.electrician.idUser = :userId\s
            \s""")
    Optional<Team> findByUserUUID(UUID userId);


}
