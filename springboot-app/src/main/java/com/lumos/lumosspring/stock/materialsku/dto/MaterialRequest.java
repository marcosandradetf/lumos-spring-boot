package com.lumos.lumosspring.stock.materialsku.dto;

import java.util.List;

public record MaterialRequest(
        Long materialId,
        String materialBaseName,
        String materialName,
        Long materialType,
        Long materialSubtype,
        String materialFunction,
        String materialModel,
        String materialBrand,
        String materialAmps,
        String materialLength,
        String materialWidth,
        String materialPower,
        String materialGauge,
        String materialWeight,
        String barCode,
        List<Long> contractItems
) {

}