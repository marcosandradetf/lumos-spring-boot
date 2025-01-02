package com.lumos.lumosspring.stock.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "tb_companies")
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_company")
    private Long idCompany;
    @Column(columnDefinition = "TEXT", unique = true, nullable = false)
    private String companyName;

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
}
