package com.lumos.lumosspring.dto.reservation;

import java.math.BigDecimal;

public record ResponseItemReserve(
        Long materialIdReservation, String description, BigDecimal reservedQuantity,
        BigDecimal quantityCompleted, Long directExecutionId, Long preMeasurementStreetId,
        String itemName
) {
}
