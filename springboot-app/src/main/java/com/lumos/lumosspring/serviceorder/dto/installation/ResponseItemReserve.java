package com.lumos.lumosspring.serviceorder.dto.installation;

import java.math.BigDecimal;

public record ResponseItemReserve(
        Long materialIdReservation, String description, BigDecimal reservedQuantity,
        BigDecimal quantityCompleted, Long directExecutionId, Long preMeasurementStreetId,
        String itemName
) {
}
