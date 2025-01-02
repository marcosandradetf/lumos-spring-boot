package com.lumos.lumosspring.stock.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "tb_groups")
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_group")
    private long idGroup;

    @Column(columnDefinition = "TEXT", unique = true, nullable = false)
    private String groupName;

    public long getIdGroup() {
        return idGroup;
    }

    public void setIdGroup(long idGroup) {
        this.idGroup = idGroup;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}
