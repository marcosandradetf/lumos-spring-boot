package com.lumos.lumosspring.company.controller;

import com.lumos.lumosspring.company.model.Company;
import com.lumos.lumosspring.company.service.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/company")
public class CompanyController {
    @Autowired
    private CompanyService companyService;

    @GetMapping
    public Iterable<Company> getAll() {
        return companyService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Company> getById(@PathVariable Long id) {
        Company empresa = companyService.findById(id);
        return empresa != null ? ResponseEntity.ok(empresa) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public Company create(@RequestBody Company empresa) {
        return companyService.save(empresa);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Company> update(@PathVariable Long id, @RequestBody Company empresa) {
        if (companyService.findById(id) != null) {
            empresa.setIdCompany(id);
            return ResponseEntity.ok(companyService.save(empresa));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (companyService.findById(id) != null) {
            companyService.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
