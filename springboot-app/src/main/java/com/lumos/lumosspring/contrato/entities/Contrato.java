package com.lumos.lumosspring.contrato.entities;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_contratos")
public class Contrato {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contrato")
    private long idContrato;

    private String numeroContrato;
    private String editalContrato;
    private Instant creationDate;
    private BigDecimal valorContrato;
    private String city;
    private String uf;

    // um contrato pode ter múltiplos itens
    @OneToMany(mappedBy = "contrato", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContratoItem> contratoItens = new ArrayList<>();

    public void addItemContrato(ContratoItem item) {
        contratoItens.add(item);
        item.setContrato(this);
    }

    public long getIdContrato() {
        return idContrato;
    }

    public void setIdContrato(long idContrato) {
        this.idContrato = idContrato;
    }

    public String getNumeroContrato() {
        return numeroContrato;
    }

    public void setNumeroContrato(String numeroContrato) {
        this.numeroContrato = numeroContrato;
    }

    public String getEditalContrato() {
        return editalContrato;
    }

    public void setEditalContrato(String editalContrato) {
        this.editalContrato = editalContrato;
    }

    public Instant  getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Instant  creationDate) {
        this.creationDate = creationDate;
    }

    public BigDecimal getValorContrato() {
        return valorContrato;
    }

    public void setValorContrato(BigDecimal valorContrato) {
        this.valorContrato = valorContrato;
    }

    public List<ContratoItem> getContratoItens() {
        return contratoItens;
    }

    public void setContratoItens(List<ContratoItem> contratoItens) {
        this.contratoItens = contratoItens;
    }

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
