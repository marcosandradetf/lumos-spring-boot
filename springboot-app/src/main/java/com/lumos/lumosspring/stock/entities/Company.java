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
    private String socialReason;
    private String bucketFileName;


    private String companyCnpj;
    private String companyContact;
    private String companyPhone;
    private String companyEmail;
    private String companyAddress;
    private String companyLogo;
    private String fantasyName;


    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<MaterialStock> materialStocks;

    public Long getIdCompany() {
        return idCompany;
    }

    public void setIdCompany(Long idEmpresa) {
        this.idCompany = idEmpresa;
    }

    public String getSocialReason() {
        return socialReason;
    }

    public void setSocialReason(String nomeEmpresa) {
        this.socialReason = nomeEmpresa;
    }

    public Set<MaterialStock> getMaterialStocks() {
        return materialStocks;
    }

    public void setMaterialStocks(Set<MaterialStock> productStocks) {
        this.materialStocks = productStocks;
    }

    public String getBucketFileName() {
        return bucketFileName;
    }

    public void setBucketFileName(String bucketFileName) {
        this.bucketFileName = bucketFileName;
    }

    public String getCompanyCnpj() {
        return companyCnpj;
    }

    public void setCompanyCnpj(String companyCnpj) {
        this.companyCnpj = companyCnpj;
    }

    public String getCompanyContact() {
        return companyContact;
    }

    public void setCompanyContact(String companyContact) {
        this.companyContact = companyContact;
    }

    public String getCompanyPhone() {
        return companyPhone;
    }

    public void setCompanyPhone(String companyPhone) {
        this.companyPhone = companyPhone;
    }

    public String getCompanyEmail() {
        return companyEmail;
    }

    public void setCompanyEmail(String companyEmail) {
        this.companyEmail = companyEmail;
    }

    public String getCompanyAddress() {
        return companyAddress;
    }

    public void setCompanyAddress(String companyAddress) {
        this.companyAddress = companyAddress;
    }

    public String getCompanyLogo() {
        return companyLogo;
    }

    public void setCompanyLogo(String companyLogo) {
        this.companyLogo = companyLogo;
    }

    public String getFantasyName() {
        return fantasyName;
    }

    public void setFantasyName(String fantasyName) {
        this.fantasyName = fantasyName;
    }
}
