package com.lumos.lumosspring.authentication.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lumos.lumosspring.authentication.entities.RefreshToken;
import com.lumos.lumosspring.user.User;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByUser(User user);
    Optional<List<RefreshToken>> findByUser(User user);

    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiryDate < :now OR t.revoked = true")
    void deleteExpiredOrRevokedTokens(@Param("now") Instant now);

}
