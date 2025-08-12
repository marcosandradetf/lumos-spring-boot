package com.lumos.lumosspring.dto.user;

import java.util.List;

public record CreateUserDto(String userId, String username, String name, String lastname,
                            String email, String cpf, int year, int month, int day,
                            List<String> role, boolean status) { }