package com.lumos.lumosspring.stock.service;

import com.lumos.lumosspring.stock.controller.dto.DepositDTO;
import com.lumos.lumosspring.stock.controller.dto.DepositResponse;
import com.lumos.lumosspring.stock.controller.dto.mobile.DepositResponseMobile;
import com.lumos.lumosspring.stock.entities.Deposit;
import com.lumos.lumosspring.stock.repository.CompanyRepository;
import com.lumos.lumosspring.stock.repository.DepositRepository;
import com.lumos.lumosspring.stock.repository.MaterialRepository;
import com.lumos.lumosspring.team.Region;
import com.lumos.lumosspring.team.RegionRepository;
import com.lumos.lumosspring.team.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DepositService {
    @Autowired
    private DepositRepository depositRepository;
    @Autowired
    private CompanyRepository comapanyRepository;
    @Autowired
    private MaterialRepository materialRepository;
    @Autowired
    private RegionRepository regionRepository;
    @Autowired
    private TeamRepository teamRepository;

    public List<DepositResponse> findAll() {
        var deposits =  depositRepository.findAllByOrderByIdDeposit();
        List<DepositResponse> depositResponses = new ArrayList<>();
        String companyName;
        String depositRegion;

        for (var deposit : deposits) {
            // Verifica se o campo 'company' é nulo
            if (deposit.getCompany() != null) {
                companyName = deposit.getCompany().getCompanyName();
            } else {
                companyName = "Não definido";  // Valor padrão
            }

            if (deposit.getRegion() != null) {
                depositRegion = deposit.getRegion().getRegionName();
            } else {
                depositRegion = "Não definido";  // Valor padrão
            }

            depositResponses.add(new DepositResponse(
                    deposit.getIdDeposit(),
                    deposit.getDepositName(),
                    companyName,
                    deposit.getDepositAddress(),
                    deposit.getDepositDistrict(),
                    deposit.getDepositCity(),
                    deposit.getDepositState(),
                    depositRegion,
                    deposit.getDepositPhone()
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
        deposit.setDepositAddress(depositDTO.depositAddress());
        deposit.setDepositDistrict(depositDTO.depositDistrict());
        deposit.setDepositCity(depositDTO.depositCity());
        deposit.setDepositState(depositDTO.depositState());
        deposit.setDepositPhone(depositDTO.depositPhone());

        if (depositDTO.depositRegion() != null && !depositDTO.depositRegion().isEmpty()) {
            var region = regionRepository.findRegionByRegionName(depositDTO.depositRegion());
            if (region.isPresent()) {
                deposit.setRegion(region.get());
            } else {
                var newRegion = new Region();
                newRegion.setRegionName(depositDTO.depositRegion());
                regionRepository.save(newRegion);
                deposit.setRegion(newRegion);
            }
        }

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
        deposit.setDepositAddress(depositDTO.depositAddress());
        deposit.setDepositDistrict(depositDTO.depositDistrict());
        deposit.setDepositCity(depositDTO.depositCity());
        deposit.setDepositState(depositDTO.depositState());
        deposit.setDepositPhone(depositDTO.depositPhone());

        if (depositDTO.depositRegion() != null && !depositDTO.depositRegion().isEmpty()) {
            var region = regionRepository.findRegionByRegionName(depositDTO.depositRegion());
            if (region.isPresent()) {
                deposit.setRegion(region.get());
            } else {
                var newRegion = new Region();
                newRegion.setRegionName(depositDTO.depositRegion());
                regionRepository.save(newRegion);
                deposit.setRegion(newRegion);
            }
        }

        depositRepository.save(deposit);

        return ResponseEntity.ok(this.findAll());
    }

    public ResponseEntity<?> delete(Long id) {
        var deposit = depositRepository.findById(id).orElse(null);
        if (deposit == null) {
            return ResponseEntity.notFound().build();
        }

        if (materialRepository.existsDeposit(id).isPresent()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Não é possível excluir: há materiais associados a este almoxarifado."));
        }

        depositRepository.delete(deposit);
        return ResponseEntity.ok(this.findAll());
    }

    public ResponseEntity<List<DepositResponseMobile>> findAllForMobile() {
        var deposits =  depositRepository.findAllByOrderByIdDeposit();
        List<DepositResponseMobile> depositResponses = new ArrayList<>();
        String companyName;
        String depositRegion;

        for (var deposit : deposits) {
            // Verifica se o campo 'company' é nulo
            if (deposit.getCompany() != null) {
                companyName = deposit.getCompany().getCompanyName();
            } else {
                companyName = "Não definido";  // Valor padrão
            }

            if (deposit.getRegion() != null) {
                depositRegion = deposit.getRegion().getRegionName();
            } else {
                depositRegion = "Não definido";  // Valor padrão
            }

            depositResponses.add(new DepositResponseMobile(
                    deposit.getIdDeposit(),
                    deposit.getDepositName(),
                    depositRegion,
                    companyName
            ));
        }
        return ResponseEntity.ok(depositResponses);
    }

}
