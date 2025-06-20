package com.lumos.lumosspring.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByUserId(UUID uuid);
    Optional<AppUser> findByUsernameIgnoreCase(String username);
    Optional<AppUser> findByEmailIgnoreCase(String email);
    Optional<AppUser> findByCpfIgnoreCase(String cpf);
    Optional<AppUser> findByUsernameOrEmailIgnoreCase(String username, String email);
    Optional<AppUser> findByUsernameOrCpfIgnoreCase(String username, String cpf);

    List<AppUser> findByStatusTrueOrderByNameAsc();

}
