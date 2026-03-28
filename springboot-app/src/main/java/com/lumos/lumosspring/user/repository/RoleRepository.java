package com.lumos.lumosspring.user.repository;

import com.lumos.lumosspring.user.model.Role;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RoleRepository extends CrudRepository<Role, Long> {
    // Nova consulta no repositório
    @Query("""
        SELECT r.role_name\s
        FROM user_role ur
        JOIN role r ON ur.id_role = r.role_id
        WHERE ur.id_user = :userId
   \s""")
    List<String> findDescriptionRolesByUserId(@Param("userId") UUID userId);


    @Query("""
        SELECT r.*
        FROM user_role ur
        JOIN role r ON ur.id_role = r.role_id
        WHERE ur.id_user = :userId
   \s""")
    List<Role> findRolesByUserId(@Param("userId") UUID userId);


}
