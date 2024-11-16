package com.lumos.lumosspring.stock.service;

import com.lumos.lumosspring.authentication.repository.RefreshTokenRepository;
import com.lumos.lumosspring.authentication.repository.UserRepository;
import com.lumos.lumosspring.stock.controller.dto.StockMovementDTO;
import com.lumos.lumosspring.stock.entities.StockMovement;
import com.lumos.lumosspring.stock.repository.*;
import com.lumos.lumosspring.util.Util;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

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


    public ResponseEntity<?> updateStock(StockMovementDTO stockMovementRequest, String refreshToken) {
        ResponseEntity<String> validationError = validateStockMovementRequest(stockMovementRequest);
        if (validationError != null) {
            return validationError;
        }

        StockMovement newStockMovement = convertToStockMovement(stockMovementRequest, util.getUserFromRToken(refreshToken).getIdUser());
        stockMovementRepository.save(newStockMovement);
        return ResponseEntity.ok("Estoque atualizado com sucesso.");
    }



    private ResponseEntity<String> validateStockMovementRequest(StockMovementDTO movement) {
        if (!supplierRepository.existsById(movement.supplierId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Fornecedor não encontrado.");
        }
        return null; // Indica que não houve erros
    }

    private StockMovement convertToStockMovement(StockMovementDTO movement, UUID userUUID) {
        var date = ZonedDateTime.now(
                ZoneId.of( "America/Sao_Paulo" )
        );
        var newMovement = new StockMovement(date.toInstant());

        newMovement.setStockMovementDescription(movement.description());
        newMovement.setInputQuantity(movement.inputQuantity());
        newMovement.setBuyUnit(movement.buyUnit());
        newMovement.setQuantityPackage(movement.quantityPackage());
        newMovement.setPricePerItem(util.convertToBigDecimal(movement.pricePerItem()));
        newMovement.setUserUpdate(userRepository.findById(userUUID).orElse(null));
        newMovement.setMaterial(materialRepository.findById(movement.materialId()).orElse(null));
        newMovement.setSupplier(supplierRepository.findById(movement.supplierId()).orElse(null));

        return newMovement;
    }



}
