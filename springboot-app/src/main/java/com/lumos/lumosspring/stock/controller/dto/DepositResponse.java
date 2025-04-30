package com.lumos.lumosspring.stock.controller.dto;

public record DepositResponse(Long idDeposit, String depositName, String companyName, String depositAddress,
                              String depositDistrict, String depositCity, String depositState, String depositRegion, String depositPhone) {
}
