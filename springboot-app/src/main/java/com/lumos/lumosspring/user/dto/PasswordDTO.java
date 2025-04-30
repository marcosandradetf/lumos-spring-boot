package com.lumos.lumosspring.user.dto;

import java.util.List;

public record PasswordDTO(String oldPassword, String password, String passwordConfirm) {
}
