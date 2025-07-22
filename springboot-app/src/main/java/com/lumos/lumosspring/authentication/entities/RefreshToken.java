package com.lumos.lumosspring.authentication.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table(name = "refresh_token")
public class RefreshToken {
    @Id
    private long idToken;

    private String token;

    private Instant expiryDate;

    private boolean revoked;

    @Column("id_user")
    private UUID appUser;

    public long getIdToken() {
        return idToken;
    }

    public void setIdToken(long idToken) {
        this.idToken = idToken;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Instant getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Instant expiryDate) {
        this.expiryDate = expiryDate;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public UUID getUser() {
        return appUser;
    }

    public void setUser(UUID appUser) {
        this.appUser = appUser;
    }
}
