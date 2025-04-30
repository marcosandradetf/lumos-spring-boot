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
        OPERADOR(3L),
        RESPONSAVEL_TECNICO(4L),
        ELETRICISTA(5L),
        MOTORISTA(6L),
        ESTOQUISTA(7L),
        ESTOQUISTA_CHEFE(8L);


        final long roleId;

        Values(long roleId) {
            this.roleId = roleId;
        }

        public long getRoleId() {
            return roleId;
        }

    }
}
