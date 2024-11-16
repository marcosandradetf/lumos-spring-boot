package com.lumos.lumosspring.stock.controller.dto;

public record MaterialRequest(String materialName, String materialBrand, String buyUnit, String requestUnit,
                              Integer stockQt, Boolean inactive, Long materialType, Long deposit, Long company) {

}
