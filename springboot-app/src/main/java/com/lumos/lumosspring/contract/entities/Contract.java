package com.lumos.lumosspring.contract.entities;

import com.lumos.lumosspring.execution.entities.PreMeasurementStreetItem;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_contracts")
public class Contract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contract")
    private long idContract;

    private String contractNumber;
    private String contractDoc;
    private Instant creationDate;
    private BigDecimal contractValue;
    private String city;
    private String uf;

    // um contrato pode ter múltiplos itens
//    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<PreMeasurementStreetItem> itemsContract = new ArrayList<>();
//
//    public void addItemContrato(PreMeasurementStreetItem preMeasurementStreetItem) {
//        itemsContract.add(preMeasurementStreetItem);
////        preMeasurementStreetItem.setContract(this);
//    }

    public long getIdContract() {
        return idContract;
    }

    public void setIdContract(long idContrato) {
        this.idContract = idContrato;
    }

    public String getContractNumber() {
        return contractNumber;
    }

    public void setContractNumber(String numeroContrato) {
        this.contractNumber = numeroContrato;
    }

    public String getContractDoc() {
        return contractDoc;
    }

    public void setContractDoc(String editalContrato) {
        this.contractDoc = editalContrato;
    }

    public Instant  getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Instant  creationDate) {
        this.creationDate = creationDate;
    }

    public BigDecimal getContractValue() {
        return contractValue;
    }

    public void setContractValue(BigDecimal valorContrato) {
        this.contractValue = valorContrato;
    }
//
//    public List<PreMeasurementStreetItem> getItemsContract() {
//        return itemsContract;
//    }
//
//    public void setItemsContract(List<PreMeasurementStreetItem> contratoItens) {
//        this.itemsContract = contratoItens;
//    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }
}
