package com.lumos.lumosspring.authentication.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.lumos.lumosspring.user.AppUser;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.lumos.lumosspring.authentication.entities.RefreshToken;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    Optional<List<RefreshToken>> findByAppUser(UUID appUser);

//    @Query("DELETE FROM refresh_token WHERE expiry_date < :now OR revoked = true")
//    void deleteExpiredOrRevokedTokens(@Param("now") Instant now);

}
