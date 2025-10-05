package com.lumos.lumosspring.stock.deposit.dto;

public record DepositResponse(
        Long idDeposit,
        String depositName,
        String companyName,
        String depositAddress,
        String depositDistrict,
        String depositCity,
        String depositState,
        String depositRegion,
        String depositPhone,
        boolean isTruck,
        String teamName,
        String plateVehicle
) {
}
