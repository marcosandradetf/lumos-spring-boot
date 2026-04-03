package com.lumos.lumosspring.stock.deposit.service;

import com.lumos.lumosspring.stock.deposit.controller.StockistController.StockistRequest;
import com.lumos.lumosspring.stock.deposit.dto.StockistModel;
import com.lumos.lumosspring.stock.deposit.repository.DepositRepository;
import com.lumos.lumosspring.team.model.Stockist;
import com.lumos.lumosspring.team.repository.RegionRepository;
import com.lumos.lumosspring.team.repository.StockistRepository;
import com.lumos.lumosspring.user.repository.UserRepository;
import com.lumos.lumosspring.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StockistService {

    @Autowired
    private StockistRepository stockistRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepositRepository depositRepository;

    @Autowired
    private RegionRepository regionRepository;

    private List<StockistModel> loadStockistDtos() {
        List<StockistModel> stockistsDto = new ArrayList<>();

        List<Stockist> allStockists = stockistRepository.findAllByTenantId(
                Utils.getCurrentTenantId()
        );

        List<Stockist> distinctStockists = allStockists.stream()
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
                    .orElseThrow(() -> new Utils.BusinessException("Usuário não encontrado."));
            var deposit = depositRepository.findById(stockist.getDepositId())
                    .orElseThrow(() -> new Utils.BusinessException("Depósito não encontrado."));

            depositName = deposit.getDepositName();
            depositAddress = deposit.getDepositAddress();
            depositPhone = deposit.getDepositPhone();
            region = regionRepository.findById(deposit.getRegion())
                    .orElseThrow(() -> new Utils.BusinessException("Região do depósito não encontrada."))
                    .getRegionName();

            stockistsDto.add(new StockistModel(
                    stockist.getUserId().toString(),
                    user.getCompletedName(),
                    deposit.getIdDeposit(),
                    depositName,
                    depositAddress,
                    depositPhone,
                    region
            ));
        }

        return stockistsDto;
    }

    public ResponseEntity<?> getStockists() {
        return ResponseEntity.ok(loadStockistDtos());
    }

    public List<StockistModel> create(StockistRequest dto) {
        if (dto == null || dto.userIdUser() == null || dto.depositIdDeposit() == null) {
            throw new Utils.BusinessException("userIdUser e depositIdDeposit são obrigatórios.");
        }

        UUID userId = dto.userIdUser();
        Long depositId = dto.depositIdDeposit();

        if (userRepository.findById(userId).isEmpty()) {
            throw new Utils.BusinessException("Usuário não encontrado.");
        }

        if (depositRepository.findById(depositId).isEmpty()) {
            throw new Utils.BusinessException("Depósito não encontrado.");
        }

        boolean exists = stockistRepository.findAllByUserId(userId)
                .stream()
                .anyMatch(s -> s.getDepositId() == depositId);

        if (exists) {
            throw new Utils.BusinessException("Este relacionamento entre usuário e depósito já existe.");
        }

        Stockist stockist = new Stockist(
                null,
                depositId,
                userId,
                UUID.randomUUID()
        );
        stockist.setTenantId(Utils.getCurrentTenantId());

        stockistRepository.save(stockist);

        return loadStockistDtos();
    }

    public List<StockistModel> update(StockistRequest dto) {
        if (dto == null || dto.userIdUser() == null || dto.depositIdDeposit() == null) {
            throw new Utils.BusinessException("userIdUser e depositIdDeposit são obrigatórios.");
        }

        UUID userId = dto.userIdUser();
        Long depositId = dto.depositIdDeposit();

        var existingStockists = stockistRepository.findAllByUserId(userId);
        if (existingStockists.isEmpty()) {
            throw new Utils.BusinessException("Nenhum stockist encontrado para este usuário.");
        }

        if (depositRepository.findById(depositId).isEmpty()) {
            throw new Utils.BusinessException("Depósito não encontrado.");
        }

        for (var stockist : existingStockists) {
            Stockist updated = new Stockist(
                    stockist.getStockistId(),
                    depositId,
                    stockist.getUserId(),
                    stockist.getNotificationCode()
            );
            updated.setTenantId(Utils.getCurrentTenantId());
            stockistRepository.save(updated);
        }

        return loadStockistDtos();
    }
}
