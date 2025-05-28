package com.lumos.lumosspring.authentication.dto;

public record LoginResponseMobile(String accessToken, Long expiresIn, String roles, String teams, String refreshToken, String userUUID) {
}
