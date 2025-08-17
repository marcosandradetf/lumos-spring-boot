package com.lumos.lumosspring.user;

import com.lumos.lumosspring.dto.user.OperationalUserResponse;
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
    List<AppUser> findByStatusTrueOrderByNameAsc();

    @Query("""
        select distinct au.user_id, au."name" || ' ' || au.last_name as complete_name
        from app_user au
        join user_role ur on ur.id_user = au.user_id
        join role r on r.role_id = ur.id_role
        where r.role_name = 'ELETRICISTA' or r.role_name = 'MOTORISTA'
    """)
    List<OperationalUserResponse> getOperationalUsers();
}
