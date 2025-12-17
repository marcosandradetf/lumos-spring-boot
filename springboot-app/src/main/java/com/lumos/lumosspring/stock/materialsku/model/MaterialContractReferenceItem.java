package com.lumos.lumosspring.stock.materialsku.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

@Table
public class MaterialContractReferenceItem implements Persistable<Long> {
    @Id
    private Long materialId;
    private Long contractReferenceItemId;

    @Transient
    private boolean isNewEntry = false;

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public Long getContractReferenceItemId() {
        return contractReferenceItemId;
    }

    public void setContractReferenceItemId(Long contractReferenceItemId) {
        this.contractReferenceItemId = contractReferenceItemId;
    }

    @Override
    public Long getId() {
        return materialId;
    }

    @Override
    public boolean isNew() {
        return isNewEntry;
    }

    public boolean isNewEntry() {
        return isNewEntry;
    }

    public void setNewEntry(boolean newEntry) {
        isNewEntry = newEntry;
    }

}