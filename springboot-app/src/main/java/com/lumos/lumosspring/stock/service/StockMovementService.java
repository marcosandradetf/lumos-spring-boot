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

    public StockMovementService(MaterialStockRepository materialStockRepository1, StockMovementRepository stockMovementRepository, SupplierRepository supplierRepository, UserRepository userRepository, RefreshTokenRepository refreshTokenRepository, JwtDecoder jwtDecoder, Util util) {
        this.materialStockRepository = materialStockRepository1;
        this.stockMovementRepository = stockMovementRepository;
        this.supplierRepository = supplierRepository;
        this.userRepository = userRepository;
        this.util = util;
    }

    public ResponseEntity<?> stockMovementGet() {
        // Busca todos os movimentos de estoque
        List<StockMovement> stockMovements = this.stockMovementRepository.findAll();

        // Filtra os movimentos de estoque com status PENDING
        List<StockMovement> pendingMovements = stockMovements.stream()
                .filter(m -> m.getStatus().equals(StockMovement.Status.PENDING))
                .toList();

        // Verifica se não existem movimentos pendentes
        if (pendingMovements.isEmpty()) {
            return new ResponseEntity<>("Nenhum movimento pendente foi encontrado!", HttpStatus.NO_CONTENT);
        }

        // Criação da lista de resposta
        List<StockMovementResponse> response = new ArrayList<>();
        for (StockMovement movement : pendingMovements) {
            // Formatação de preço para substituir ponto por vírgula
            String employee = movement.getUserCreated().getName().concat(" ")
                    .concat(movement.getUserCreated().getLastName());


            // Marca do material (evitando NullPointerException)
            String brand = movement.getMaterialStock().getMaterial().getMaterialBrand();
            brand = (brand != null && !brand.isEmpty()) ? " (" + brand + ") " : "";

            // Descrição do material (prioridade: materialPower > materialLength > materialAmps)
            String description = movement.getMaterialStock().getMaterial().getMaterialPower();

            if (description == null || description.isEmpty()) {
                description = movement.getMaterialStock().getMaterial().getMaterialLength();
            }

            if (description == null || description.isEmpty()) {
                description = movement.getMaterialStock().getMaterial().getMaterialAmps();
            }

            // Se ainda for null, define como string vazia
            description = (description != null) ? " - " + description : "";


            // Adiciona o movimento de estoque na resposta
            response.add(new StockMovementResponse(
                    movement.getStockMovementId(),
                    movement.getStockMovementDescription(),
                    movement.getMaterialStock().getMaterial().getMaterialName().concat(description).concat(brand),
                    movement.getTotalQuantity(),
                    movement.getBuyUnit(),
                    movement.getRequestUnit(),
                    movement.getPricePerItem().toString(),
                    movement.getSupplier().getSupplierName(),
                    movement.getMaterialStock().getCompany().getSocialReason(),
                    movement.getMaterialStock().getDeposit().getDepositName(),
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
        return convertToStockMovementAndSave(stockMovementRequest, util.getUserFromRToken(refreshToken).getUserId());

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
            var existingMovement = stockMovementRepository.findFirstByMaterial(material.get(), StockMovement.Status.APPROVED);
            if (existingMovement.isPresent()) {
                // Se o movimento existente tem um tipo de compra diferente, retorna erro
                if (!existingMovement.get().getRequestUnit().equals(movement.requestUnit())) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Não será possível criar o movimento para o material ".concat(movement.materialId().toString()).concat(" pois já existe histórico com a unidade de requisição ").concat(existingMovement.get().getRequestUnit()));
                }
            }

            var newMovement = new StockMovement(util.getDateTime());

            newMovement.setStockMovementDescription(movement.description());
            newMovement.setInputQuantity(movement.inputQuantity());
            newMovement.setBuyUnit(movement.buyUnit());
            newMovement.setBuyRequest(movement.requestUnit());
            newMovement.setQuantityPackage(movement.quantityPackage());
            newMovement.setTotalQuantity(movement.totalQuantity());
            newMovement.setPricePerItem(util.convertToBigDecimal(movement.priceTotal()), movement.totalQuantity());
            newMovement.setPriceTotal(util.convertToBigDecimal(movement.priceTotal()));
            newMovement.setUserCreated(userRepository.findByUserId(userUUID).orElse(null));
            newMovement.setMaterialStock(material.get());
            newMovement.setSupplier(supplierRepository.findById(Long.parseLong(movement.supplierId())).orElse(null));
            newMovement.setStatus(StockMovement.Status.PENDING);
            stockMovementRepository.save(newMovement);
        }

        return ResponseEntity.status(HttpStatus.OK).body("Movimento criado com sucesso.");

    }

    public ResponseEntity<String> approveStockMovement(long movementId, String refreshToken) {
        var movement = stockMovementRepository.findById(movementId).orElse(null);
        if (movement == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Movimento não encontrado.");
        } else if (Objects.equals(movement.getStatus(), StockMovement.Status.REJECTED)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Não é possível aprovar pois Movimento já foi rejeitado.");
        } else if (Objects.equals(movement.getStatus(), StockMovement.Status.APPROVED)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Não é possível aprovar pois Movimento já foi aprovado.");
        }
        movement.setStatus(StockMovement.Status.APPROVED);
        movement.setUserFinished(util.getUserFromRToken(refreshToken));
        movement.materialUpdate();
        stockMovementRepository.save(movement);
        return ResponseEntity.status(HttpStatus.OK).body("Movimento aprovado com sucesso.");
    }

    public ResponseEntity<String> rejectStockMovement(long movementId, String refreshToken) {
        var movement = stockMovementRepository.findById(movementId).orElse(null);
        if (movement == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Movimento não encontrado.");
        } else if (Objects.equals(movement.getStatus(), StockMovement.Status.APPROVED)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Não é possível reprovar pois Movimento já foi aprovado.");
        }
        movement.setStatus(StockMovement.Status.REJECTED);
        movement.setUserFinished(util.getUserFromRToken(refreshToken));
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
                .filter(m -> m.getStatus().equals(StockMovement.Status.APPROVED))
                .toList();

        // Verifica se não existem movimentos aprovado
        if (approvedMovements.isEmpty()) {
            return new ResponseEntity<>("Nenhum movimento aprovado foi encontrado!", HttpStatus.NO_CONTENT);
        }

        // Criação da lista de resposta
        List<StockMovementResponse> response = new ArrayList<>();
        for (StockMovement movement : approvedMovements) {
            // Formatação de preço para substituir ponto por vírgula
            String employee = movement.getUserFinished().getName().concat(" ")
                    .concat(movement.getUserCreated().getLastName());
            // Marca do material (evitando NullPointerException)

            String brand = movement.getMaterialStock().getMaterial().getMaterialBrand();
            brand = (brand != null && !brand.isEmpty()) ? " (" + brand + ") " : "";

            // Descrição do material (prioridade: materialPower > materialLength > materialAmps)
            String description = movement.getMaterialStock().getMaterial().getMaterialPower();

            if (description == null || description.isEmpty()) {
                description = movement.getMaterialStock().getMaterial().getMaterialLength();
            }

            if (description == null || description.isEmpty()) {
                description = movement.getMaterialStock().getMaterial().getMaterialAmps();
            }

            // Se ainda for null, define como string vazia
            description = (description != null) ? " - " + description : "";

            // Adiciona o movimento de estoque na resposta
            response.add(new StockMovementResponse(
                    movement.getStockMovementId(),
                    movement.getStockMovementDescription(),
                    movement.getMaterialStock().getMaterial().getMaterialName().concat(description).concat(brand),
                    movement.getTotalQuantity(),
                    movement.getBuyUnit(),
                    movement.getRequestUnit(), // Note que este valor aparece duas vezes, verifique se é necessário
                    movement.getPricePerItem().toString(),
                    movement.getSupplier().getSupplierName(),
                    movement.getMaterialStock().getCompany().getSocialReason(),
                    movement.getMaterialStock().getDeposit().getDepositName(),
                    employee
            ));
        }

        // Retorna a resposta com status OK
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
