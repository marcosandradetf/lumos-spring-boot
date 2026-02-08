package com.lumos.lumosspring.company.service;

import com.lumos.lumosspring.company.dto.CompanyRequest;
import com.lumos.lumosspring.company.model.Company;
import com.lumos.lumosspring.company.repository.CompanyRepository;
import com.lumos.lumosspring.s3.service.S3Service;
import com.lumos.lumosspring.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CompanyService {
    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private S3Service s3Service;

    @Cacheable(
            value = "getAllCompanies",
            key = "T(com.lumos.lumosspring.util.Utils).getCurrentTenantId()"
    )
    public Iterable<Company> findAll() {
        return companyRepository.findAllByTenantId(Utils.getCurrentTenantId());
    }

    public Company findById(Long id) {
        return companyRepository.findById(id).orElse(null);
    }

    public ResponseEntity<Long> save(CompanyRequest req, MultipartFile logo) {
        var logoUri = this.s3Service.uploadFile(logo, Utils.getCurrentBucket(), "photos/logo", req.fantasyName(), Utils.getCurrentTenantId());

        var company = new Company(
                req.socialReason(),
                req.companyCnpj(),
                req.companyContact(),
                req.companyPhone(),
                req.companyEmail(),
                req.companyAddress(),
                logoUri,
                req.fantasyName()
        );

        company = this.companyRepository.save(company);

        return ResponseEntity.ok(company.getIdCompany());
    }

    public void deleteById(Long id) {
        companyRepository.deleteById(id);
    }
}
