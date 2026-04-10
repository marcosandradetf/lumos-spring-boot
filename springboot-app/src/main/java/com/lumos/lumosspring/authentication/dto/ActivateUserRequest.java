package com.lumos.lumosspring.authentication.dto;

public record ActivateUserRequest(String cpf, String activationCode, String newPassword) {
}
