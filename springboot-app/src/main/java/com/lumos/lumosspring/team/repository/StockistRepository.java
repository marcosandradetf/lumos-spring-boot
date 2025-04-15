package com.lumos.lumosspring.team.repository;

import com.lumos.lumosspring.team.entities.Stockist;
import com.lumos.lumosspring.team.entities.Team;
import com.lumos.lumosspring.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockistRepository extends JpaRepository<Stockist, Long> {
    @Query("""
                SELECT s FROM Stockist s\s
                WHERE s.user.idUser = :userId\s
           \s""")
    Optional<Stockist> findByUserUUID(UUID userId);



}
