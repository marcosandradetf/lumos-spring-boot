package com.lumos.lumosspring.notifications.entities;

import com.lumos.lumosspring.stock.entities.Company;
import jakarta.persistence.*;

@Entity
public class EmailConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long emailId;

    private String host;
    private int port;
    private String username;
    private String password;
    private boolean auth;
    private boolean starttls;
    @OneToOne
    private Company company;

    // Getters e setters
    public Long getEmailId() {
        return emailId;
    }

    public void setEmailId(Long emailId) {
        this.emailId = emailId;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isAuth() {
        return auth;
    }

    public void setAuth(boolean auth) {
        this.auth = auth;
    }

    public boolean isStarttls() {
        return starttls;
    }

    public void setStarttls(boolean starttls) {
        this.starttls = starttls;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }
}
