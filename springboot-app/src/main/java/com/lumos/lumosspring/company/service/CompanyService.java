package com.lumos.lumosspring.company.service;

import com.lumos.lumosspring.company.dto.CompanyRequest;
import com.lumos.lumosspring.company.model.Company;
import com.lumos.lumosspring.company.repository.CompanyRepository;
import com.lumos.lumosspring.minio.service.MinioService;
import com.lumos.lumosspring.util.DefaultResponse;
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
    private MinioService minioService;

    @Cacheable("getAllCompanies")
    public Iterable<Company> findAll() {
        return companyRepository.findAll();
    }

    public Company findById(Long id) {
        return companyRepository.findById(id).orElse(null);
    }

    public ResponseEntity<Long> save(CompanyRequest req, MultipartFile logo) {
        var logoUri = this.minioService.uploadFile(logo, Utils.INSTANCE.getCurrentBucket(), "photos/logo", req.fantasyName());

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
