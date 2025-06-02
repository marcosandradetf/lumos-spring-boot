package com.lumos.lumosspring.notifications.repository;

import com.lumos.lumosspring.notifications.entities.EmailConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailConfigRepository extends JpaRepository<EmailConfig, Long> {
}
