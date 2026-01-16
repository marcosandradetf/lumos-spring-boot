package com.lumos.lumosspring.user.repository;

import com.lumos.lumosspring.user.dto.OperationalUserResponse;
import com.lumos.lumosspring.user.model.AppUser;
import org.springframework.data.jdbc.repository.query.Query;
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

    @Query("""
        select distinct au.user_id, au."name" || ' ' || au.last_name as complete_name
        from app_user au
        join user_role ur on ur.id_user = au.user_id
        join role r on r.role_id = ur.id_role
        where r.role_name = 'ELETRICISTA' or r.role_name = 'MOTORISTA'
            and au.tenant_id = :tenantId
    """)
    List<OperationalUserResponse> getOperationalUsers(UUID tenantId);

    @Query("""
        select distinct au.user_id
        from app_user au
        join user_role ur on ur.id_user = au.user_id
        join role r on r.role_id = ur.id_role
        where r.role_name = 'RESPONSAVEL_TECNICO'
            and au.tenant_id = :tenantId
    """)
    List<UUID> getResponsibleTechUsers(UUID tenantId);

    List<AppUser> findByTenantIdAndStatusTrueAndSupportFalseOrderByNameAsc(UUID tenantId, Boolean status, Boolean support);
}
