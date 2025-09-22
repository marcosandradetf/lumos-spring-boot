package com.lumos.lumosspring.pre_measurement.dto.response;

import java.math.BigDecimal;

public record PreMeasurementStreetItemResponseDTO(
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
