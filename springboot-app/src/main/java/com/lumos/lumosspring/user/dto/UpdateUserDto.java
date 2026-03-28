package com.lumos.lumosspring.user.dto;

import com.lumos.lumosspring.user.model.Role;

import java.util.List;

public record UpdateUserDto(String userId, String username, String name, String lastname,
                            String email, String cpf, int day, int month, int year,
                            List<Role> role, boolean status, boolean sel) {
}
