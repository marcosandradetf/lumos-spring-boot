package com.lumos.lumosspring.stock.service;

import com.lumos.lumosspring.authentication.repository.RefreshTokenRepository;
import com.lumos.lumosspring.authentication.repository.UserRepository;
import com.lumos.lumosspring.stock.controller.dto.StockMovementDTO;
import com.lumos.lumosspring.stock.entities.StockMovement;
import com.lumos.lumosspring.stock.repository.*;
import com.lumos.lumosspring.util.Util;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class StockMovementService {
    private final MaterialRepository materialRepository;
    private final StockMovementRepository stockMovementRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;
    private final Util util;

    enum status {
        PENDING,
        APPROVED,
        REJECTED
    }

    public StockMovementService(MaterialRepository materialRepository, StockMovementRepository stockMovementRepository, SupplierRepository supplierRepository, UserRepository userRepository, RefreshTokenRepository refreshTokenRepository, JwtDecoder jwtDecoder, Util util) {
        this.materialRepository = materialRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.supplierRepository = supplierRepository;
        this.userRepository = userRepository;
        this.util = util;
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
            if (!supplierRepository.existsById(movement.supplierId())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Fornecedor não encontrado.");
            }
        }
        return null; // Indica que não houve erros
    }

    private ResponseEntity<String> convertToStockMovementAndSave(List<StockMovementDTO> stockMovement, UUID userUUID) {
        var date = ZonedDateTime.now(
                ZoneId.of( "America/Sao_Paulo" )
        );

        for (StockMovementDTO movement : stockMovement) {
            var newMovement = new StockMovement(date.toInstant());

            newMovement.setStockMovementDescription(movement.description());
            newMovement.setInputQuantity(movement.inputQuantity());
            newMovement.setBuyUnit(movement.buyUnit());
            newMovement.setQuantityPackage(movement.quantityPackage());
            newMovement.setPricePerItem(util.convertToBigDecimal(movement.pricePerItem()));
            newMovement.setUserCreated(userRepository.findById(userUUID).orElse(null));
            newMovement.setMaterial(materialRepository.findById(movement.materialId()).orElse(null));
            newMovement.setSupplier(supplierRepository.findById(movement.supplierId()).orElse(null));
            newMovement.setStatus(status.PENDING.name());
            stockMovementRepository.save(newMovement);
        }

        return ResponseEntity.status(HttpStatus.OK).body("Movimento criado com sucesso.");

    }

    public ResponseEntity<String> approveStockMovement(long movementId, String refreshToken) {
        var movement = stockMovementRepository.findById(movementId).orElse(null);
        if (movement == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Movimento não encontrado.");
        } else if (Objects.equals(movement.getStatus(), status.REJECTED.name())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Não é possível aprovar pois Movimento já foi rejeitado.");
        } else if (Objects.equals(movement.getStatus(), status.APPROVED.name())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Não é possível aprovar pois Movimento já foi aprovado.");
        }
        movement.setStatus(status.APPROVED.name());
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
        } else if (Objects.equals(movement.getStatus(), status.APPROVED.name())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Não é possível reprovar pois Movimento já foi aprovado.");
        }
        movement.setStatus(status.REJECTED.name());
        movement.setUserFinished(util.getUserFromRToken(refreshToken));
        stockMovementRepository.save(movement);
        return ResponseEntity.status(HttpStatus.OK).body("Movimento rejeitado com sucesso.");
    }

}
