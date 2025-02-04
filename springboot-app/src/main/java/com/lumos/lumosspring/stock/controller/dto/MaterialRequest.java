package com.lumos.lumosspring.stock.controller.dto;

public record MaterialRequest(String materialName, String materialBrand, String materialPower, String materialAmps, String materialLength,
                              String buyUnit, String requestUnit, Integer stockQt, Boolean inactive, Boolean allDeposits, Long materialType, Long deposit, Long company) {

}
