package com.lumos.lumosspring.stock.controller.dto.mobile;

public record MaterialDTOMob(long materialId, String materialName, String materialBrand, String materialPower, String materialAmps, String materialLength,
                             String requestUnit, String stockQt, String companyName, Long depositId) {

}
