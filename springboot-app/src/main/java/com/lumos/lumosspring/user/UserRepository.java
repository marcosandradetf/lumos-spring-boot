package com.lumos.lumosspring.user;

import org.hibernate.annotations.processing.SQL;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByIdUser(UUID uuid);
    Optional<User> findByUsernameIgnoreCase(String username);
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByCpfIgnoreCase(String cpf);
    Optional<User> findByUsernameOrEmailIgnoreCase(String username, String email);
    Optional<User> findByUsernameOrCpfIgnoreCase(String username, String cpf);

    @Query("SELECT u.idUser FROM User u JOIN u.roles r WHERE r.roleName IN :roleNames")
    Optional<List<UUID>> findAllByRoleNames(@Param("roleNames") Set<String> roleNames);

    List<User> findByStatusTrueOrderByNameAsc();

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.roleName IN :roleNames")
    List<User> getUsersByRoles(@Param("roleNames") List<String> roleNames);
}
