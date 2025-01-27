package com.lumos.lumosspring.authentication.dto;

public record LoginRequest(String username, String email, String password) {
}
