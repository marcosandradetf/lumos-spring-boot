package com.lumos.lumosspring.stock.materialreference.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table
public class MaterialGroup {
    @Id
    private Long idGroup;

    private String groupName;

    public MaterialGroup(Long idGroup, String groupName) {
        this.idGroup = idGroup;
        this.groupName = groupName;
    }

    public MaterialGroup() {}

    public Long getIdGroup() {
        return idGroup;
    }

    public void setIdGroup(Long idGroup) {
        this.idGroup = idGroup;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}
