package com.lumos.lumosspring.stock.service;

import com.lumos.lumosspring.stock.controller.dto.DepositDTO;
import com.lumos.lumosspring.stock.controller.dto.DepositResponse;
import com.lumos.lumosspring.stock.entities.Deposit;
import com.lumos.lumosspring.stock.entities.Type;
import com.lumos.lumosspring.stock.repository.CompanyRepository;
import com.lumos.lumosspring.stock.repository.DepositRepository;
import com.lumos.lumosspring.stock.repository.MaterialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class DepositService {
    @Autowired
    private DepositRepository depositRepository;
    @Autowired
    private CompanyRepository comapanyRepository;
    @Autowired
    private MaterialRepository materialRepository;

    public List<DepositResponse> findAll() {
        var deposits =  depositRepository.findAllByOrderByIdDeposit();
        List<DepositResponse> depositResponses = new ArrayList<>();
        String companyName;

        for (var deposit : deposits) {
            // Verifica se o campo 'company' é nulo
            if (deposit.getCompany() != null) {
                companyName = deposit.getCompany().getCompanyName();
            } else {
                // Se 'company' for nulo, define um valor padrão ou pode lançar uma exceção
                companyName = "Não definido";  // Valor padrão
                // Ou lançar uma exceção se preferir, por exemplo:
                // throw new IllegalArgumentException("O depósito " + deposit.getIdDeposit() + " não tem empresa associada.");
            }
            depositResponses.add(new DepositResponse(
                    deposit.getIdDeposit(),
                    deposit.getDepositName(),
                    companyName
            ));
        }
        return depositResponses;
    }

    public Deposit findById(Long id) {
        return depositRepository.findById(id).orElse(null);
    }

    public ResponseEntity<?> save(DepositDTO depositDTO) {
        if (depositRepository.existsByDepositName(depositDTO.depositName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Collections.singletonMap("message", "Este almoxarifado já existe."));
        }

        var deposit = new Deposit();
        deposit.setDepositName(depositDTO.depositName());
        deposit.setCompany(comapanyRepository.findById(depositDTO.companyId()).orElse(null));
        depositRepository.save(deposit);

        return ResponseEntity.ok(this.findAll());
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

        return ResponseEntity.ok(this.findAll());
    }

    public ResponseEntity<?> delete(Long id) {
        var type = depositRepository.findById(id).orElse(null);
        if (type == null) {
            return ResponseEntity.notFound().build();
        }

        if (materialRepository.existsDeposit(id).isPresent()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Não é possível excluir: há materiais associados a este almoxarifado."));
        }

        depositRepository.delete(type);
        return ResponseEntity.ok(this.findAll());
    }

}
