package com.lumos.lumosspring.user.dto;

import java.util.List;

public record UpdateUserDto(String userId, String username, String name, String lastname,
                            String email, String cpf, int day, int month, int year,
                            List<String> role, boolean status, boolean sel) {
}
