package com.lumos.lumosspring.dto.user;

public record PasswordDTO(String oldPassword, String password, String passwordConfirm) {
}
