package com.lumos.lumosspring.authentication.controller.dto;

public record LoginResponse(String accessToken, Long expiresIn) {
}
