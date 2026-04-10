package com.lumos.lumosspring.user.dto;

import com.lumos.lumosspring.user.model.Role;
import com.lumos.lumosspring.user.model.UserStatus;

import java.util.List;

public record UpdateUserDto(String userId, String username, String name, String lastname,
                            String email, String cpf, int day, int month, int year,
                            List<Role> role, UserStatus status, boolean sel) {
}
