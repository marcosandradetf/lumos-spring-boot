package com.lumos.lumosspring.authentication.dto;

public record LoginResponse(String accessToken, Long expiresIn, String roles) {
}
