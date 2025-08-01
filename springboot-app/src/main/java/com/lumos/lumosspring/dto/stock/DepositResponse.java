package com.lumos.lumosspring.dto.stock;

public record DepositResponse(Long idDeposit, String depositName, String companyName, String depositAddress,
                              String depositDistrict, String depositCity, String depositState, String depositRegion, String depositPhone) {
}
