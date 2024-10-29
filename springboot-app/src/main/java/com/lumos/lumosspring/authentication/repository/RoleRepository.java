package com.lumos.lumosspring.authentication.repository;

import com.lumos.lumosspring.authentication.entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Role findByNomeRole(String nomeRole);
}
