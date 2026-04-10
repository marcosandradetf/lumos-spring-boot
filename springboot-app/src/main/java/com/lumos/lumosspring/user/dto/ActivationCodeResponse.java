package com.lumos.lumosspring.user.dto;

import java.time.Instant;

public record ActivationCodeResponse(String activationCode, Instant expiresAt, String message) {
}
