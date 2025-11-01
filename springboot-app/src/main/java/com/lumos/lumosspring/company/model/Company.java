package com.lumos.lumosspring.company.model;

import com.lumos.lumosspring.authentication.model.TenantEntity;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table
public class Company extends TenantEntity {
    @Id
    private Long idCompany;

    private String socialReason;
    private String companyCnpj;
    private String companyContact;
    private String companyPhone;
    private String companyEmail;
    private String companyAddress;
    private String companyLogo;
    private String fantasyName;

    public Company() {}

    public Company(String socialReason,  String companyCnpj, String companyContact, String companyPhone, String companyEmail, String companyAddress, String companyLogo, String fantasyName) {
        this.socialReason = socialReason;
        this.companyCnpj = companyCnpj;
        this.companyContact = companyContact;
        this.companyPhone = companyPhone;
        this.companyEmail = companyEmail;
        this.companyAddress = companyAddress;
        this.companyLogo = companyLogo;
        this.fantasyName = fantasyName;
    }

    public Long getIdCompany() {
        return idCompany;
    }

    public void setIdCompany(Long idCompany) {
        this.idCompany = idCompany;
    }

    public String getSocialReason() {
        return socialReason;
    }

    public void setSocialReason(String socialReason) {
        this.socialReason = socialReason;
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
