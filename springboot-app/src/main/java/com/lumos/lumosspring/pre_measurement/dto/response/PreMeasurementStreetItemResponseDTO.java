package com.lumos.lumosspring.pre_measurement.dto.response;

public record PreMeasurementStreetItemResponseDTO(
        long preMeasurementStreetItemId,
        long contractItemId,
        String contractReferenceItemName,
        String contractReferenceNameForImport,
        String contractReferenceItemType,
        String contractReferenceLinking,
        String contractReferenceItemDependency,
        double measuredQuantity,
        String itemStatus
) {
}
