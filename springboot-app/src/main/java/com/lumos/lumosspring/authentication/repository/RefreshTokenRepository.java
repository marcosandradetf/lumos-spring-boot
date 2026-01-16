package com.lumos.lumosspring.authentication.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import com.lumos.lumosspring.authentication.model.RefreshToken;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends CrudRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    Optional<List<RefreshToken>> findByAppUser(UUID appUser);

    void deleteByToken(String token);

//    @Query("DELETE FROM refresh_token WHERE expiry_date < :now OR revoked = true")
//    void deleteExpiredOrRevokedTokens(@Param("now") Instant now);

}
