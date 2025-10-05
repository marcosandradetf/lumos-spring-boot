package com.lumos.lumosspring.premeasurement.dto.premeasurement;


import java.math.BigDecimal;

public record CheckBalanceResponse(
        String description,
        BigDecimal totalMeasured,
        BigDecimal totalBalance,
        BigDecimal totalContractedQuantity,
        BigDecimal totalQuantityExecuted,
        BigDecimal totalCurrentBalance
) {
}
