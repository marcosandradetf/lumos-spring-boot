package com.lumos.lumosspring.authentication.entities;

import com.lumos.lumosspring.user.AppUser;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "refresh_token")
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long idToken;

    @Column(unique = true, nullable = false, columnDefinition = "TEXT")
    private String token;

    @Column(nullable = false)
    private Instant expiryDate;

    @Column(nullable = false)
    private boolean revoked;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user", nullable = false)
    private AppUser appUser;

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

    public AppUser getUser() {
        return appUser;
    }

    public void setUser(AppUser appUser) {
        this.appUser = appUser;
    }
}
