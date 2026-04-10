package com.lumos.lumosspring.user.dto;

import com.lumos.lumosspring.user.model.Role;
import com.lumos.lumosspring.user.model.UserStatus;

import java.time.Instant;
import java.util.List;

public record UserResponse(String userId, String username, String name, String lastname, String email, String cpf, List<Role> role,
                           int year, int month, int day, UserStatus status, boolean mustChangePassword,
                           Instant activationExpiresAt) {
}
