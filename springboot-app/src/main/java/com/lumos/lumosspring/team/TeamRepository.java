package com.lumos.lumosspring.team;

import com.lumos.lumosspring.user.User;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface TeamRepository extends CrudRepository<Team, Long> {
    Optional<Team> findByUser(User user);
}
