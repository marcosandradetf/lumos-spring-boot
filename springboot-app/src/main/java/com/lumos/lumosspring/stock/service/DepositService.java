package com.lumos.lumosspring.stock.service;

import com.lumos.lumosspring.stock.controller.dto.DepositDTO;
import com.lumos.lumosspring.stock.entities.Deposit;
import com.lumos.lumosspring.stock.entities.Type;
import com.lumos.lumosspring.stock.repository.CompanyRepository;
import com.lumos.lumosspring.stock.repository.DepositRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepositService {
    @Autowired
    private DepositRepository depositRepository;

    @Autowired
    private CompanyRepository comapanyRepository;

    public List<Deposit> findAll() {
        return depositRepository.findAll();
    }

    public Deposit findById(Long id) {
        return depositRepository.findById(id).orElse(null);
    }

    public ResponseEntity<?> save(DepositDTO depositDTO) {
        var deposit = new Deposit();
        deposit.setDepositName(depositDTO.depositName());
        deposit.setCompany(comapanyRepository.findById(depositDTO.companyId()).orElse(null));
        depositRepository.save(deposit);

        return ResponseEntity.ok(depositRepository.findAll());
    }

    public ResponseEntity<?> update(Long depositId, DepositDTO depositDTO) {
        var deposit = depositRepository.findById(depositId).orElse(null);;
        var company = comapanyRepository.findById(depositDTO.companyId()).orElse(null);;
        if (deposit == null) {
            return ResponseEntity.notFound().build();
        }
        if (company == null) {
            return ResponseEntity.notFound().build();
        }

        deposit.setDepositName(depositDTO.depositName());
        deposit.setCompany(company);
        depositRepository.save(deposit);

        return ResponseEntity.ok(depositRepository.findAll());
    }

    public ResponseEntity<?> delete(Long id) {
        var type = depositRepository.findById(id).orElse(null);
        if (type == null) {
            return ResponseEntity.notFound().build();
        }
        depositRepository.delete(type);
        return ResponseEntity.ok(depositRepository.findAll());
    }

}
