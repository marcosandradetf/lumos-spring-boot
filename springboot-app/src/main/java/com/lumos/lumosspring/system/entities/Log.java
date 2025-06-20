package com.lumos.lumosspring.system.entities;

import com.lumos.lumosspring.user.AppUser;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
public class Log {
    @Id
    @Column(name = "id_log")
    private long idLog;

    private String message;

    @ManyToOne
    @JoinColumn(name = "id_user")
    private AppUser appUser;

    @CreationTimestamp
    private Instant creationTimestamp;

    private String type;

    private String category;

    public long getIdLog() {
        return idLog;
    }

    public void setIdLog(long idLog) {
        this.idLog = idLog;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public AppUser getUser() {
        return appUser;
    }

    public void setUser(AppUser appUser) {
        this.appUser = appUser;
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
