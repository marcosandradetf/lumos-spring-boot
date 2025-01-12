package com.lumos.lumosspring.authentication.dto;

public record LoginResponseMobile(String accessToken, Long expiresIn, String roles, String refreshToken) {
}
