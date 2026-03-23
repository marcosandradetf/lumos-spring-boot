package com.lumos.lumosspring.notifications.repository;

import com.lumos.lumosspring.notifications.model.EmailQueue;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailQueueRepository extends CrudRepository<EmailQueue, Long> {
    List<EmailQueue> findAllByStatus(String status);
}
