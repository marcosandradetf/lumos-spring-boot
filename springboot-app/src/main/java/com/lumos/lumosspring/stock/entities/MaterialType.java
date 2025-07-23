package com.lumos.lumosspring.stock.entities;


import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table
public class MaterialType {
    @Id
    private Long idType;

    private String typeName;

    private Long idGroup;

    public MaterialType(Long idType, String typeName, Long idGroup) {
        this.idType = idType;
        this.typeName = typeName;
        this.idGroup = idGroup;
    }

    public MaterialType() {}

    public Long getIdType() {
        return idType;
    }

    public void setIdType(Long idType) {
        this.idType = idType;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public Long getIdGroup() {
        return idGroup;
    }

    public void setIdGroup(Long idGroup) {
        this.idGroup = idGroup;
    }
}
