package com.lumos.lumosspring.notifications.repository;

import com.lumos.lumosspring.notifications.entities.EmailConfig;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailConfigRepository extends CrudRepository<EmailConfig, Long> {
}
