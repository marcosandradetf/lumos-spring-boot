package com.lumos.lumosspring.user.model;

import com.lumos.lumosspring.authentication.dto.LoginRequest;

import java.time.LocalDate;
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

    private Boolean status;

    private Long teamId;

    private Boolean support;

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

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
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

    @Override
    public UUID getId() {
        return userId;
    }

    @Override
    public boolean isNew() {
        return isNewEntry;
    }
}