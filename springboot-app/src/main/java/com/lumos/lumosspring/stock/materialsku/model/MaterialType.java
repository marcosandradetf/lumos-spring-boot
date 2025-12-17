package com.lumos.lumosspring.stock.materialsku.model;


import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table
public class MaterialType {
    @Id
    private Long idType;

    private String typeName;

    private Long idGroup;

    // new columns
    private Boolean isFractionable;
    private Boolean usePower;
    private Boolean useLength;
    private Boolean useGauge;

    public MaterialType() {
    }

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
        switch (typeName) {
            case "fita",
                 "fita isolante autofusão",
                 "fita isolante adesivo",
                 "fita isolante" -> this.isFractionable = true;

            case "led",
                 "refletor",
                 "refletor led",
                 "lâmpada" -> this.usePower = true;

            case "cabo" -> {
                this.isFractionable = true;
                this.useGauge = true;
            }

            case "braço",
                 "poste" -> this.useLength = true;

        }
    }

    public Long getIdGroup() {
        return idGroup;
    }

    public void setIdGroup(Long idGroup) {
        this.idGroup = idGroup;
    }
}
