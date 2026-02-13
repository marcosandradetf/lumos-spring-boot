package com.lumos.lumosspring.premeasurement.dto;

import java.math.BigDecimal;

public record PreMeasurementStreetItemResponse(
        long preMeasurementStreetItemId,
        long contractItemId,
        String contractReferenceItemName,
        String contractReferenceNameForImport,
        String contractReferenceItemType,
        String contractReferenceLinking,
        String contractReferenceItemDependency,
        BigDecimal measuredQuantity,
        String itemStatus
) {
}
