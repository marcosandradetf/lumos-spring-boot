package com.lumos.lumosspring.company.dto;

public record CompanyRequest(
    String socialReason,
    String fantasyName,
    String companyCnpj,
    String companyContact,
    String companyPhone,
    String companyEmail,
    String companyAddress
) {
}
