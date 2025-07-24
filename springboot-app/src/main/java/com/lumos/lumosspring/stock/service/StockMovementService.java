package com.lumos.lumosspring.stock.service;

import com.lumos.lumosspring.authentication.repository.RefreshTokenRepository;
import com.lumos.lumosspring.user.UserRepository;
import com.lumos.lumosspring.stock.controller.dto.StockMovementDTO;
import com.lumos.lumosspring.stock.controller.dto.StockMovementResponse;
import com.lumos.lumosspring.stock.entities.StockMovement;
import com.lumos.lumosspring.stock.repository.*;
import com.lumos.lumosspring.util.Util;
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
    private final MaterialStockRepository materialStockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;
    private final Util util;
    private final MaterialRepository materialRepository;
    private final CompanyRepository companyRepository;
    private final DepositRepository depositRepository;

    public StockMovementService(MaterialStockRepository materialStockRepository1, StockMovementRepository stockMovementRepository, SupplierRepository supplierRepository, UserRepository userRepository, RefreshTokenRepository refreshTokenRepository, JwtDecoder jwtDecoder, Util util, MaterialRepository materialRepository, CompanyRepository companyRepository, DepositRepository depositRepository) {
        this.materialStockRepository = materialStockRepository1;
        this.stockMovementRepository = stockMovementRepository;
        this.supplierRepository = supplierRepository;
        this.userRepository = userRepository;
        this.util = util;
        this.materialRepository = materialRepository;
        this.companyRepository = companyRepository;
        this.depositRepository = depositRepository;
    }

    public ResponseEntity<?> stockMovementGet() {
        // Busca todos os movimentos de estoque
        var stockMovements = this.stockMovementRepository.findAllByStatus("PENDING");

        // Criação da lista de resposta
        List<StockMovementResponse> response = new ArrayList<>();
        for (StockMovement movement : stockMovements) {
            var userCreated = userRepository.findByUserId(movement.getAppUserCreatedId()).orElseThrow();
            var materialStock = materialStockRepository.findById(movement.getMaterialStockId()).orElseThrow();
            var material = materialRepository.findById(materialStock.getMaterialId()).orElseThrow();
            var supplier = supplierRepository.findById(movement.getSupplierId()).orElseThrow();
            var company = companyRepository.findById(materialStock.getCompanyId()).orElseThrow();
            var deposit = depositRepository.findById(materialStock.getDepositId()).orElseThrow();

            // Formatação de preço para substituir ponto por vírgula
            String employee = userCreated.getCompletedName();


            // Marca do material (evitando NullPointerException)
            String brand = "";

            // Descrição do material (prioridade: materialPower > materialLength > materialAmps)
            String description = material.getMaterialPower();

            if (description == null || description.isEmpty()) {
                description = material.getMaterialLength();
            }

            if (description == null || description.isEmpty()) {
                description = material.getMaterialAmps();
            }

            // Se ainda for null, define como string vazia
            description = (description != null) ? " - " + description : "";


            // Adiciona o movimento de estoque na resposta
            response.add(new StockMovementResponse(
                    movement.getStockMovementId(),
                    movement.getStockMovementDescription(),
                    material.getMaterialName().concat(description).concat(brand),
                    movement.getTotalQuantity(),
                    movement.getBuyUnit(),
                    movement.getRequestUnit(),
                    movement.getPricePerItem().toString(),
                    supplier.getSupplierName(),
                    company.getSocialReason(),
                    deposit.getDepositName(),
                    employee
            ));
        }

        // Retorna a resposta com status OK
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    public ResponseEntity<?> createMovement(List<StockMovementDTO> stockMovementRequest, String refreshToken) {
        ResponseEntity<String> validationError = validateStockMovementRequest(stockMovementRequest);
        if (validationError != null) {
            return validationError;
        }
        return convertToStockMovementAndSave(stockMovementRequest, util.getUserFromRToken(refreshToken));

    }

    private ResponseEntity<String> validateStockMovementRequest(List<StockMovementDTO> stockMovement) {
        for (StockMovementDTO movement : stockMovement) {
            if (!supplierRepository.existsById(Long.parseLong(movement.supplierId()))) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Fornecedor não encontrado.");
            }
        }
        return null; // Indica que não houve erros
    }

    private ResponseEntity<String> convertToStockMovementAndSave(List<StockMovementDTO> stockMovement, UUID userUUID) {
        for (StockMovementDTO movement : stockMovement) {
            var material = materialStockRepository.findById(movement.materialId());
            if (material.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Material ".concat(movement.materialId().toString()).concat(" não encontrado."));
            }

            // Verificar se já existe um movimento de estoque para o material
            var existingMovement = stockMovementRepository.findFirstByMaterial(material.get().getMaterialIdStock(), "APPROVED");
            if (existingMovement.isPresent()) {
                // Se o movimento existente tem um tipo de compra diferente, retorna erro
                if (!existingMovement.get().getRequestUnit().equals(movement.requestUnit())) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Não será possível criar o movimento para o material ".concat(movement.materialId().toString()).concat(" pois já existe histórico com a unidade de requisição ").concat(existingMovement.get().getRequestUnit()));
                }
            }

            var newMovement = new StockMovement();

            newMovement.setStockMovementRefresh(Instant.now());
            newMovement.setStockMovementDescription(movement.description());
            newMovement.setInputQuantity(movement.inputQuantity());
            newMovement.setBuyUnit(movement.buyUnit());
            newMovement.setRequestUnit(movement.requestUnit());
            newMovement.setQuantityPackage(movement.quantityPackage());
            newMovement.setTotalQuantity(movement.totalQuantity());
            newMovement.setPricePerItem(util.convertToBigDecimal(movement.priceTotal()));
            newMovement.setPriceTotal(util.convertToBigDecimal(movement.priceTotal()));
            newMovement.setAppUserCreatedId(userUUID);
            newMovement.setMaterialStockId(material.get().getMaterialIdStock());
            newMovement.setSupplierId(Long.valueOf(movement.supplierId()));
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

        var materialStock = materialStockRepository.findById(movement.getMaterialStockId()).orElseThrow();

        materialStock.addStockQuantity(movement.getTotalQuantity());
        materialStock.addStockAvailable(movement.getTotalQuantity());
        materialStock.setCostPerItem(movement.getPricePerItem());
        materialStock.setCostPrice(movement.getPriceTotal());
        materialStock.setBuyUnit(movement.getBuyUnit());
        materialStock.setRequestUnit(movement.getRequestUnit());


        stockMovementRepository.save(movement);
        materialStockRepository.save(materialStock);

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

        List<StockMovement> stockMovements = this.stockMovementRepository.findApprovedBetweenDates(startInstant, endDate);

        // Filtra os movimentos de estoque com status PENDING
        List<StockMovement> approvedMovements = stockMovements.stream()
                .filter(m -> m.getStatus().equals("APPROVED"))
                .toList();

        // Verifica se não existem movimentos aprovado
        if (approvedMovements.isEmpty()) {
            return new ResponseEntity<>("Nenhum movimento aprovado foi encontrado!", HttpStatus.NO_CONTENT);
        }

        // Criação da lista de resposta
        List<StockMovementResponse> response = new ArrayList<>();
        for (StockMovement movement : approvedMovements) {
            var userFinished = userRepository.findByUserId(movement.getAppUserFinishedId()).orElseThrow();
            var materialStock = materialStockRepository.findById(movement.getMaterialStockId()).orElseThrow();
            var material = materialRepository.findById(materialStock.getMaterialId()).orElseThrow();
            var supplier = supplierRepository.findById(movement.getSupplierId()).orElseThrow();
            var company = companyRepository.findById(materialStock.getCompanyId()).orElseThrow();
            var deposit = depositRepository.findById(materialStock.getDepositId()).orElseThrow();

            // Formatação de preço para substituir ponto por vírgula
            String employee = userFinished.getCompletedName();
            // Marca do material (evitando NullPointerException)

            String brand = "";
            //brand = (brand != null && !brand.isEmpty()) ? " (" + brand + ") " : "";

            // Descrição do material (prioridade: materialPower > materialLength > materialAmps)
            String description = material.getMaterialPower();

            if (description == null || description.isEmpty()) {
                description = material.getMaterialLength();
            }

            if (description == null || description.isEmpty()) {
                description = material.getMaterialAmps();
            }

            // Se ainda for null, define como string vazia
            description = (description != null) ? " - " + description : "";

            // Adiciona o movimento de estoque na resposta
            response.add(new StockMovementResponse(
                    movement.getStockMovementId(),
                    movement.getStockMovementDescription(),
                    material.getMaterialName().concat(description).concat(brand),
                    movement.getTotalQuantity(),
                    movement.getBuyUnit(),
                    movement.getRequestUnit(), // Note que este valor aparece duas vezes, verifique se é necessário
                    movement.getPricePerItem().toString(),
                    supplier.getSupplierName(),
                    company.getSocialReason(),
                    deposit.getDepositName(),
                    employee
            ));
        }

        // Retorna a resposta com status OK
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
