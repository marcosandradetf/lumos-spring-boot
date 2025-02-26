package com.lumos.lumosspring.stock.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.Set;

@Entity
@Table(name = "tb_companies")
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_company")
    private Long idCompany;
    @Column(columnDefinition = "TEXT", unique = true, nullable = false)
    private String companyName;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<MaterialStock> materialStocks;

    public Long getIdCompany() {
        return idCompany;
    }

    public void setIdCompany(Long idEmpresa) {
        this.idCompany = idEmpresa;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String nomeEmpresa) {
        this.companyName = nomeEmpresa;
    }

    public Set<MaterialStock> getMaterialStocks() {
        return materialStocks;
    }

    public void setMaterialStocks(Set<MaterialStock> productStocks) {
        this.materialStocks = productStocks;
    }
}
