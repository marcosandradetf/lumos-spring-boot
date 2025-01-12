package com.lumos.lumosspring.stock.service;

import com.lumos.lumosspring.authentication.RefreshTokenRepository;
import com.lumos.lumosspring.user.repository.UserRepository;
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
    private final MaterialRepository materialRepository;
    private final StockMovementRepository stockMovementRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;
    private final Util util;

    public StockMovementService(MaterialRepository materialRepository, StockMovementRepository stockMovementRepository, SupplierRepository supplierRepository, UserRepository userRepository, RefreshTokenRepository refreshTokenRepository, JwtDecoder jwtDecoder, Util util) {
        this.materialRepository = materialRepository;
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
            String formattedPrice = this.util.formatPrice(movement.getPricePerItem());
            String employee = movement.getUserCreated().getName().concat(" ")
                    .concat(movement.getUserCreated().getLastName());

            // Adiciona o movimento de estoque na resposta
            response.add(new StockMovementResponse(
                    movement.getStockMovementId(),
                    movement.getStockMovementDescription(),
                    movement.getMaterial().getMaterialName(),
                    movement.getInputQuantity(),
                    movement.getBuyUnit(),
                    movement.getInputQuantity(), // Note que este valor aparece duas vezes, verifique se é necessário
                    formattedPrice,
                    movement.getSupplier().getSupplierName(),
                    movement.getMaterial().getCompany().getCompanyName(),
                    movement.getMaterial().getDeposit().getDepositName(),
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
        return convertToStockMovementAndSave(stockMovementRequest, util.getUserFromRToken(refreshToken).getIdUser());

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
            var newMovement = new StockMovement(util.getDateTime());

            newMovement.setStockMovementDescription(movement.description());
            newMovement.setInputQuantity(movement.inputQuantity());
            newMovement.setBuyUnit(movement.buyUnit());
            newMovement.setQuantityPackage(movement.quantityPackage());
            newMovement.setPricePerItem(util.convertToBigDecimal(movement.pricePerItem()));
            newMovement.setUserCreated(userRepository.findByIdUser(userUUID).orElse(null));
            newMovement.setMaterial(materialRepository.findById(movement.materialId()).orElse(null));
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
            String formattedPrice = this.util.formatPrice(movement.getPricePerItem());
            String employee = movement.getUserFinished().getName().concat(" ")
                    .concat(movement.getUserCreated().getLastName());

            // Adiciona o movimento de estoque na resposta
            response.add(new StockMovementResponse(
                    movement.getStockMovementId(),
                    movement.getStockMovementDescription(),
                    movement.getMaterial().getMaterialName(),
                    movement.getInputQuantity(),
                    movement.getBuyUnit(),
                    movement.getInputQuantity(), // Note que este valor aparece duas vezes, verifique se é necessário
                    formattedPrice,
                    movement.getSupplier().getSupplierName(),
                    movement.getMaterial().getCompany().getCompanyName(),
                    movement.getMaterial().getDeposit().getDepositName(),
                    employee
            ));
        }

        // Retorna a resposta com status OK
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
