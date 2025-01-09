package com.lumos.lumosspring.authentication.controller.dto;

public record LoginResponseMobile(String accessToken, Long expiresIn, String roles, String refreshToken) {
}
