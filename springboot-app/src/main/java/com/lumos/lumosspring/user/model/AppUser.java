package com.lumos.lumosspring.user.model;

import com.lumos.lumosspring.authentication.dto.LoginRequest;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.lumos.lumosspring.authentication.model.TenantEntity;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.security.crypto.password.PasswordEncoder;

@Table
public class AppUser extends TenantEntity implements Persistable<UUID> {
    @Id
    private UUID userId;

    @Transient
    private boolean isNewEntry = false;

    private String username;

    private String name;

    private String lastName;

    private String email;

    private String password;

    private String phoneNumber;

    private String codeResetPassword;

    private String cpf;

    private LocalDate dateOfBirth;

    private UserStatus status;

    private Boolean mustChangePassword;

    private String activationCodeHash;

    private Instant activationCodeExpiresAt;

    private Integer activationAttemptCount;

    private Long teamId;

    private Boolean support;

    private OffsetDateTime createdAt;

    private OffsetDateTime deactivatedAt;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID idUser) {
        this.userId = idUser;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    public boolean isLoginCorrect(LoginRequest loginRequest, PasswordEncoder passwordEncoder) {
        return passwordEncoder.matches(loginRequest.password(), this.password);
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getName() {
        return name;
    }

    public String getCompletedName() {
        return name.concat(" ").concat(lastName);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getCodeResetPassword() {
        return codeResetPassword;
    }

    public void setCodeResetPassword(String codeResetPassword) {
        this.codeResetPassword = codeResetPassword;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public Boolean getMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(Boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public String getActivationCodeHash() {
        return activationCodeHash;
    }

    public void setActivationCodeHash(String activationCodeHash) {
        this.activationCodeHash = activationCodeHash;
    }

    public Instant getActivationCodeExpiresAt() {
        return activationCodeExpiresAt;
    }

    public void setActivationCodeExpiresAt(Instant activationCodeExpiresAt) {
        this.activationCodeExpiresAt = activationCodeExpiresAt;
    }

    public Integer getActivationAttemptCount() {
        return activationAttemptCount;
    }

    public void setActivationAttemptCount(Integer activationAttemptCount) {
        this.activationAttemptCount = activationAttemptCount;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Long getTeamId() {
        return teamId;
    }

    public boolean isNewEntry() {
        return isNewEntry;
    }

    public void setNewEntry(boolean newEntry) {
        isNewEntry = newEntry;
    }

    public Boolean getSupport() {
        return support;
    }

    public void setSupport(Boolean support) {
        this.support = support;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getDeactivatedAt() {
        return deactivatedAt;
    }

    public void setDeactivatedAt(OffsetDateTime deactivatedAt) {
        this.deactivatedAt = deactivatedAt;
    }

    @Override
    public UUID getId() {
        return userId;
    }

    @Override
    public boolean isNew() {
        return isNewEntry;
    }
}
