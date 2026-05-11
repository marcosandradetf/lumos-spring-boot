package com.lumos.lumosspring.authentication.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table
public class Tenant implements Persistable<UUID> {
    @Id
    private UUID tenantId;
    @ReadOnlyProperty
    private Long tenantNumber;
    private String description;
    private String bucket;

    @Transient
    private boolean isNewEntry = false;


    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public Long getTenantNumber() {
        return tenantNumber;
    }

    public void setTenantNumber(Long tenantNumber) {
        this.tenantNumber = tenantNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public boolean isNewEntry() {
        return isNewEntry;
    }

    public void setNewEntry(boolean newEntry) {
        isNewEntry = newEntry;
    }

    @Override
    public UUID getId() {
        return tenantId;
    }

    @Override
    public boolean isNew() {
        return isNewEntry;
    }
}
