package com.lumos.lumosspring.authentication.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

public class QrcodeToken implements Persistable<UUID> {
    @Id
    private UUID token;

    @Transient
    private boolean isNewEntry = false;

    private UUID userId;

    private Instant expiresAt;

    public UUID getToken() {
        return token;
    }

    public void setToken(UUID token) {
        this.token = token;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isNewEntry() {
        return isNewEntry;
    }

    public void setNewEntry(boolean newEntry) {
        isNewEntry = newEntry;
    }

    @Override
    public UUID getId() {
        return token;
    }

    @Override
    public boolean isNew() {
        return isNewEntry;
    }
}
