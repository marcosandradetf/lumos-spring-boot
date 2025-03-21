package com.lumos.lumosspring.user;

import jakarta.persistence.*;

@Entity
@Table(name = "tb_roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long roleId;

    @Column(unique = true)
    private String roleName;

    public long getRoleId() {
        return roleId;
    }

    public void setRoleId(long idRole) {
        this.roleId = idRole;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public enum Values {
        ADMIN(1L),
        ANALISTA(2L),
        RESPONSAVEL_TECNICO(3L),
        ELETRICISTA(4L),
        MOTORISTA(5L),
        ESTOQUISTA(6L),
        ESTOQUISTA_CHEFE(7L);


        final long roleId;

        Values(long roleId) {
            this.roleId = roleId;
        }

        public long getRoleId() {
            return roleId;
        }

    }
}
