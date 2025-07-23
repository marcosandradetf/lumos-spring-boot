package com.lumos.lumosspring.stock.service;

import com.lumos.lumosspring.stock.DepositByStockist;
import com.lumos.lumosspring.stock.StockistModel;
import com.lumos.lumosspring.stock.controller.dto.DepositDTO;
import com.lumos.lumosspring.stock.controller.dto.DepositResponse;
import com.lumos.lumosspring.stock.entities.Deposit;
import com.lumos.lumosspring.stock.repository.CompanyRepository;
import com.lumos.lumosspring.stock.repository.DepositRepository;
import com.lumos.lumosspring.stock.repository.MaterialStockRepository;
import com.lumos.lumosspring.team.entities.Region;
import com.lumos.lumosspring.team.entities.Stockist;
import com.lumos.lumosspring.team.repository.RegionRepository;
import com.lumos.lumosspring.team.repository.StockistRepository;
import com.lumos.lumosspring.user.UserRepository;
import com.lumos.lumosspring.util.JdbcUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
public class DepositService {
    @Autowired
    private DepositRepository depositRepository;
    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private MaterialStockRepository materialStockRepository;
    @Autowired
    private RegionRepository regionRepository;
    @Autowired
    private StockistRepository stockistRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private NamedParameterJdbcTemplate namedJdbc;

    @Cacheable("getAllDeposits")
    public List<DepositResponse> findAll() {
        var deposits = depositRepository.findAllByOrderByIdDeposit();
        List<DepositResponse> depositResponses = new ArrayList<>();
        String companyName;
        String depositRegion;

        for (var deposit : deposits) {
            // Verifica se o campo 'company' é nulo
            if (deposit.getCompanyId() != null) {
                companyName = companyRepository.findById(deposit.getCompanyId()).orElseThrow().getSocialReason();
            } else {
                companyName = "Não definido";  // Valor padrão
            }

            if (deposit.getRegion() != null) {
                depositRegion =  regionRepository.findById(deposit.getRegion()).orElseThrow().getRegionName();
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
        deposit.setCompanyId(depositDTO.companyId());
        deposit.setDepositAddress(depositDTO.depositAddress());
        deposit.setDepositDistrict(depositDTO.depositDistrict());
        deposit.setDepositCity(depositDTO.depositCity());
        deposit.setDepositState(depositDTO.depositState());
        deposit.setDepositPhone(depositDTO.depositPhone());

        if (depositDTO.depositRegion() != null && !depositDTO.depositRegion().isEmpty()) {
            var region = regionRepository.findRegionByRegionName(depositDTO.depositRegion());
            if (region.isPresent()) {
                deposit.setRegion(region.get().getRegionId());
            } else {
                var newRegion = new Region(
                        null,
                        depositDTO.depositRegion()
                );
                newRegion.setRegionName(depositDTO.depositRegion());
                regionRepository.save(newRegion);
                deposit.setRegion(newRegion.getRegionId());
            }
        }

        depositRepository.save(deposit);

        return ResponseEntity.ok(this.findAll());
    }

    public ResponseEntity<?> update(Long depositId, DepositDTO depositDTO) {
        var deposit = depositRepository.findById(depositId).orElse(null);

        if (deposit == null) {
            return ResponseEntity.notFound().build();
        }

        deposit.setDepositName(depositDTO.depositName());
        deposit.setCompanyId(depositDTO.companyId());
        deposit.setDepositAddress(depositDTO.depositAddress());
        deposit.setDepositDistrict(depositDTO.depositDistrict());
        deposit.setDepositCity(depositDTO.depositCity());
        deposit.setDepositState(depositDTO.depositState());
        deposit.setDepositPhone(depositDTO.depositPhone());

        if (depositDTO.depositRegion() != null && !depositDTO.depositRegion().isEmpty()) {
            var region = regionRepository.findRegionByRegionName(depositDTO.depositRegion());
            if (region.isPresent()) {
                deposit.setRegion(region.get().getRegionId());
            } else {
                var newRegion = new Region(
                        null,
                        depositDTO.depositRegion()
                );
                newRegion.setRegionName(depositDTO.depositRegion());
                regionRepository.save(newRegion);
                deposit.setRegion(newRegion.getRegionId());
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

        if (materialStockRepository.existsDeposit(id).isPresent()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Não é possível excluir: há materiais associados a este almoxarifado."));
        }

        depositRepository.delete(deposit);
        return ResponseEntity.ok(this.findAll());
    }

    public ResponseEntity<?> getStockists() {
        List<StockistModel> stockistsDto = new ArrayList<>();
        Iterable<Stockist> allStockists = stockistRepository.findAll();

        List<Stockist> allStockistsList = new ArrayList<>();
        allStockists.forEach(allStockistsList::add);

        List<Stockist> distinctStockists = allStockistsList.stream()
                .collect(Collectors.toMap(
                        Stockist::getUserId,
                        Function.identity(),
                        (existing, replacement) -> existing
                ))
                .values()
                .stream()
                .toList();

        for (var stockist : distinctStockists) {
            var depositName = "Não Informado no Sistema";
            var depositAddress = "Não Informado no Sistema";
            var depositPhone = "Não Informado no Sistema";
            var region = "Não Informado no Sistema";

            var user = userRepository.findById(stockist.getUserId())
                    .orElseThrow();
            var deposit = depositRepository.findById(stockist.getDepositId())
                    .orElseThrow();

            depositName = deposit.getDepositName();
            depositAddress = deposit.getDepositAddress();
            depositPhone = deposit.getDepositPhone();
            region = regionRepository.findById(deposit.getRegion()).orElseThrow().getRegionName();

            stockistsDto.add(new StockistModel(
                    stockist.getUserId().toString(),
                    user.getCompletedName(),
                    deposit.getIdDeposit(),
                    depositName, depositAddress,
                    depositPhone, region
            ));
        }

        return ResponseEntity.ok(stockistsDto);
    }

    public ResponseEntity<?> getDepositStockist(String userId) {
        List<DepositByStockist> depositsResponse = new ArrayList<>();

        var deposits = JdbcUtil.INSTANCE.getRawData(
                namedJdbc,
                """
                        select s.deposit_id_deposit, d.deposit_name, d.deposit_address, d.deposit_phone
                        from stockist s
                        inner join deposit d on d.id_deposit = s.deposit_id_deposit
                        where s.user_id_user = :userId
                        """,
                Map.of("userId", UUID.fromString(userId))
        );

        for (var deposit : deposits) {
            depositsResponse.add(new DepositByStockist(
                    ((Number) deposit.get("deposit_id_deposit")).longValue(),
                    (String) deposit.get("deposit_name"),
                    (String) deposit.get("deposit_address"),
                    (String) deposit.get("deposit_phone")
            ));
        }


        return ResponseEntity.ok(depositsResponse);
    }
}
