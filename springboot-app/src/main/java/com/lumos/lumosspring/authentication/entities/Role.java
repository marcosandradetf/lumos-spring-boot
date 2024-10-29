package com.lumos.lumosspring.authentication.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "tb_roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_role")
    private long idRole;

    @Column(unique = true)
    private String nomeRole;

    public long getIdRole() {
        return idRole;
    }

    public void setIdRole(long idRole) {
        this.idRole = idRole;
    }

    public String getNomeRole() {
        return nomeRole;
    }

    public void setNomeRole(String nomeRole) {
        this.nomeRole = nomeRole;
    }

    public enum Values {
        ADMIN(1L),
        MANAGER(2L),
        BASIC(3L);

        long idRole;

        Values(long idRole) {
            this.idRole = idRole;
        }

        public long getIdRole() {
            return idRole;
        }

    }
}
