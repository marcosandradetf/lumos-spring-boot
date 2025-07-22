package com.lumos.lumosspring.authentication.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.lumos.lumosspring.user.AppUser;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.lumos.lumosspring.authentication.entities.RefreshToken;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    Optional<List<RefreshToken>> findByAppUser(UUID appUser);

    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiryDate < :now OR t.revoked = true")
    void deleteExpiredOrRevokedTokens(@Param("now") Instant now);

}
