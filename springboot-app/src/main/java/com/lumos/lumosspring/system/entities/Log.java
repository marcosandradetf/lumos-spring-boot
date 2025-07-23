package com.lumos.lumosspring.system.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table
public class Log {
    @Id
    private Long idLog;

    private String message;

    private UUID idUser;

    private Instant creationTimestamp;

    private String type;

    private String category;

    public Log(Long idLog, String message, UUID idUser, Instant creationTimestamp, String type, String category) {
        this.idLog = idLog;
        this.message = message;
        this.idUser = idUser;
        this.creationTimestamp = creationTimestamp;
        this.type = type;
        this.category = category;
    }

    public Log() {
    }

    public Long getIdLog() {
        return idLog;
    }

    public void setIdLog(Long idLog) {
        this.idLog = idLog;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public UUID getIdUser() {
        return idUser;
    }

    public void setIdUser(UUID idUser) {
        this.idUser = idUser;
    }

    public Instant getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(Instant creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
