package com.lumos.lumosspring.stock.service;

import com.lumos.lumosspring.stock.entities.Company;
import com.lumos.lumosspring.stock.repository.CompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompanyService {
    @Autowired
    private CompanyRepository companyRepository;

    @Cacheable("getAllCompanies")
    public Iterable<Company> findAll() {
        return companyRepository.findAll();
    }

    public Company findById(Long id) {
        return companyRepository.findById(id).orElse(null);
    }

    public Company save(Company material) {
        return companyRepository.save(material);
    }

    public void deleteById(Long id) {
        companyRepository.deleteById(id);
    }
}
