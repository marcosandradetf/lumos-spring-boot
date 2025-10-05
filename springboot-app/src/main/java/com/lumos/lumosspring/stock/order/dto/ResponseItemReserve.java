package com.lumos.lumosspring.stock.order.dto;

import java.math.BigDecimal;

public record ResponseItemReserve(
        Long materialIdReservation, String description, BigDecimal reservedQuantity,
        BigDecimal quantityCompleted, Long directExecutionId, Long preMeasurementStreetId,
        String itemName
) {
}
