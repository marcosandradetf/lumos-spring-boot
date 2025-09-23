package com.lumos.lumosspring.dto.pre_measurement;


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
