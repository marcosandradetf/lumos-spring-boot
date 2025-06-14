package com.lumos.lumosspring.user;

import com.lumos.lumosspring.authentication.dto.LoginRequest;
import com.lumos.lumosspring.team.entities.Stockist;
import com.lumos.lumosspring.team.entities.Team;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;

@Entity
@Table(name = "tb_users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_user")
    private UUID idUser;

    @Column(unique = true)
    private String username;

    private String name;
    private String lastName;

    private String email;

    private String password;

    private String phoneNumber;

    private String codeResetPassword;

    @Column(unique = true)
    private String cpf;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(
            name = "tb_users_roles",
            joinColumns = @JoinColumn(name = "id_user"),
            inverseJoinColumns = @JoinColumn(name = "id_role")
    )
    private Set<Role> roles;

    private LocalDate dateOfBirth;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean status;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Stockist> stockists = new ArrayList<>();

    @OneToMany(mappedBy = "driver", fetch = FetchType.LAZY)
    private List<Team> drivers;

    @OneToMany(mappedBy = "electrician", fetch = FetchType.LAZY)
    private List<Team> electricians;


    public UUID getIdUser() {
        return idUser;
    }

    public void setIdUser(UUID idUser) {
        this.idUser = idUser;
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

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
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

    public List<Stockist> getStockists() {
        return stockists;
    }

    public void setStockists(List<Stockist> stockist) {
        this.stockists = stockist;
    }

    public List<Team> getDrivers() {
        return drivers;
    }

    public void setDrivers(List<Team> drivers) {
        this.drivers = drivers;
    }

    public List<Team> getElectricians() {
        return electricians;
    }

    public void setElectricians(List<Team> electricians) {
        this.electricians = electricians;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}