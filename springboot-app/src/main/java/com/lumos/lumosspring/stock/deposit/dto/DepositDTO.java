package com.lumos.lumosspring.stock.deposit.dto;

public record DepositDTO(String depositName, Long companyId, String depositAddress, String depositDistrict,
                         String depositCity, String depositState, String depositRegion, String depositPhone) {
}
