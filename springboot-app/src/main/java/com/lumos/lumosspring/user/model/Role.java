package com.lumos.lumosspring.user.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table
public class Role {
    @Id
    private Long roleId;

    private String roleName;
    private String label;
    private String description;

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

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public enum Values {
        ADMIN(1L),
        ANALISTA(2L),
        ELETRICISTA(3L),
        ESTOQUISTA_CHEFE(4L),
        ESTOQUISTA(5L),
        MOTORISTA(6L),
        RESPONSAVEL_TECNICO(7L);


        final long roleId;

        Values(long roleId) {
            this.roleId = roleId;
        }

        public long getRoleId() {
            return roleId;
        }



    }
}
