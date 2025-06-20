package com.lumos.lumosspring.stock.entities;

import jakarta.persistence.*;

@Entity
public class MaterialType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_type")
    private long idType;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String typeName;
    @ManyToOne
    @JoinColumn(name = "id_group", nullable = false)
    private MaterialGroup materialGroup;

    public long getIdType() {
        return idType;
    }

    public void setIdType(long idType) {
        this.idType = idType;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public MaterialGroup getGroup() {
        return materialGroup;
    }

    public void setGroup(MaterialGroup materialGroup) {
        this.materialGroup = materialGroup;
    }
}
