package com.lumos.lumosspring.user.dto;

public record PasswordDTO(String oldPassword, String password, String passwordConfirm) {
}
