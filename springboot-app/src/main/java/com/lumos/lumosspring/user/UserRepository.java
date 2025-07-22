package com.lumos.lumosspring.user;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends CrudRepository<AppUser, UUID> {
    Optional<AppUser> findByUserId(UUID uuid);
    Optional<AppUser> findByUsernameIgnoreCase(String username);
    Optional<AppUser> findByCpfIgnoreCase(String cpf);
    Optional<AppUser> findByUsernameOrCpfIgnoreCase(String username, String cpf);
    List<AppUser> findByStatusTrueOrderByNameAsc();
}
