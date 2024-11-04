package com.lumos.lumosspring.authentication.controller.dto;

import java.util.Set;

public record LoginResponse(String accessToken, Long expiresIn, String roles) {
}
