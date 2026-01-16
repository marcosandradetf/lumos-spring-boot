package com.lumos.lumosspring.stock.materialstock.service;

import com.lumos.lumosspring.authentication.repository.RefreshTokenRepository;
import com.lumos.lumosspring.company.repository.CompanyRepository;
import com.lumos.lumosspring.stock.deposit.repository.DepositRepository;
import com.lumos.lumosspring.stock.materialsku.repository.MaterialReferenceRepository;
import com.lumos.lumosspring.stock.materialstock.repository.MaterialStockRegisterRepository;
import com.lumos.lumosspring.stock.materialstock.repository.MaterialStockViewRepository;
import com.lumos.lumosspring.stock.materialstock.repository.StockMovementRepository;
import com.lumos.lumosspring.stock.materialstock.repository.SupplierRepository;
import com.lumos.lumosspring.user.repository.UserRepository;
import com.lumos.lumosspring.stock.materialstock.dto.StockMovementDTO;
import com.lumos.lumosspring.stock.materialstock.dto.StockMovementResponse;
import com.lumos.lumosspring.stock.materialstock.model.StockMovement;
import com.lumos.lumosspring.util.Util;
import com.lumos.lumosspring.util.Utils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@Service
public class StockMovementService {
    private final MaterialStockRegisterRepository materialStockRegisterRepository;
    private final StockMovementRepository stockMovementRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;
    private final Util util;
    private final MaterialReferenceRepository materialReferenceRepository;
    private final CompanyRepository companyRepository;
    private final DepositRepository depositRepository;
    private final MaterialStockViewRepository materialStockViewRepository;

    public StockMovementService(MaterialStockRegisterRepository materialStockRegisterRepository1, StockMovementRepository stockMovementRepository, SupplierRepository supplierRepository, UserRepository userRepository, RefreshTokenRepository refreshTokenRepository, JwtDecoder jwtDecoder, Util util, MaterialReferenceRepository materialReferenceRepository, CompanyRepository companyRepository, DepositRepository depositRepository, MaterialStockViewRepository materialStockViewRepository) {
        this.materialStockRegisterRepository = materialStockRegisterRepository1;
        this.stockMovementRepository = stockMovementRepository;
        this.supplierRepository = supplierRepository;
        this.userRepository = userRepository;
        this.util = util;
        this.materialReferenceRepository = materialReferenceRepository;
        this.companyRepository = companyRepository;
        this.depositRepository = depositRepository;
        this.materialStockViewRepository = materialStockViewRepository;
    }

    public ResponseEntity<?> stockMovementGet() {
        // Busca todos os movimentos de estoque
        var stockMovements = this.stockMovementRepository.findAllByStatus("PENDING",Utils.INSTANCE.getCurrentTenantId());
        // Retorna a resposta com status OK
        return new ResponseEntity<>(stockMovements, HttpStatus.OK);
    }

    public ResponseEntity<?> createMovement(List<StockMovementDTO> stockMovementRequest) {
        return convertToStockMovementAndSave(stockMovementRequest, Utils.INSTANCE.getCurrentUserId());
    }

    private ResponseEntity<String> convertToStockMovementAndSave(List<StockMovementDTO> stockMovement, UUID userUUID) {
        for (StockMovementDTO movement : stockMovement) {
            var material = materialStockRegisterRepository.findById(movement.materialStockId());
            if (material.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Material ".concat(movement.materialStockId().toString()).concat(" não encontrado."));
            }

            // Verificar se já existe um movimento de estoque para o material
            var existingMovement = stockMovementRepository.findFirstByMaterial(material.get().getMaterialIdStock(), "APPROVED");
            if (existingMovement.isPresent()) {
                // Se o movimento existente tem um tipo de compra diferente, retorna erro
                if (!existingMovement.get().getTotalQuantity().equals(movement.totalQuantity())) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Não foi possível criar o movimento para o material ".concat(movement.materialStockId().toString()).concat(" pois já existe histórico pendente com a mesma quantidade."));
                }
            }

            var newMovement = new StockMovement();
            newMovement.setStockMovementRefresh(Instant.now());
            newMovement.setStockMovementDescription(movement.description());
            newMovement.setInputQuantity(movement.inputQuantity());
            newMovement.setQuantityPackage(movement.quantityPackage());
            newMovement.setTotalQuantity(movement.totalQuantity());
            newMovement.setPricePerItem(util.convertToBigDecimal(movement.priceTotal()));
            newMovement.setPriceTotal(util.convertToBigDecimal(movement.priceTotal()));
            newMovement.setAppUserCreatedId(userUUID);
            newMovement.setMaterialStockId(material.get().getMaterialIdStock());
            newMovement.setStatus("PENDING");
            stockMovementRepository.save(newMovement);

        }

        return ResponseEntity.status(HttpStatus.OK).body("Movimento criado com sucesso.");

    }

    @Transactional
    public ResponseEntity<String> approveStockMovement(long movementId, String refreshToken) {
        var movement = stockMovementRepository.findById(movementId).orElse(null);
        if (movement == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Movimento não encontrado.");
        } else if (Objects.equals(movement.getStatus(), "REJECTED")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Não é possível aprovar pois Movimento já foi rejeitado.");
        } else if (Objects.equals(movement.getStatus(), "APPROVED")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Não é possível aprovar pois Movimento já foi aprovado.");
        }
        movement.setStatus("APPROVED");
        movement.setAppUserFinishedId(Objects.requireNonNull(util.getUserFromRToken(refreshToken)));

        var materialStock = materialStockRegisterRepository.findById(movement.getMaterialStockId()).orElseThrow();

        materialStock.addStockQuantity(movement.getTotalQuantity());
        materialStock.addStockAvailable(movement.getTotalQuantity());
        materialStock.setCostPerItem(movement.getPricePerItem());
        materialStock.setCostPrice(movement.getPriceTotal());

        stockMovementRepository.save(movement);
        materialStockRegisterRepository.save(materialStock);

        return ResponseEntity.status(HttpStatus.OK).body("Movimento aprovado com sucesso.");
    }

    public ResponseEntity<String> rejectStockMovement(long movementId, String refreshToken) {
        var movement = stockMovementRepository.findById(movementId).orElse(null);
        if (movement == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Movimento não encontrado.");
        } else if (Objects.equals(movement.getStatus(), "APPROVED")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Não é possível reprovar pois Movimento já foi aprovado.");
        }

        movement.setStatus("REJECTED");
        movement.setAppUserFinishedId(Objects.requireNonNull(util.getUserFromRToken(refreshToken)));
        stockMovementRepository.save(movement);


        return ResponseEntity.status(HttpStatus.OK).body("Movimento rejeitado com sucesso.");
    }


    public ResponseEntity<?> stockMovementGetApproved() {
        // Busca todos os movimentos de estoque
        Instant endDate = Instant.now();

        // Converter Instant para ZonedDateTime (para poder manipular meses)
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(endDate, ZoneId.systemDefault());

        // Subtrair 3 meses
        ZonedDateTime startDate = zonedDateTime.minusMonths(3);

        // Converter de volta para Instant
        Instant startInstant = startDate.toInstant();

        var stockMovements = this.stockMovementRepository.findApprovedBetweenDates(startInstant, endDate, "APPROVED", Utils.INSTANCE.getCurrentTenantId());

        // Retorna a resposta com status OK
        return new ResponseEntity<>(stockMovements, HttpStatus.OK);
    }
}
