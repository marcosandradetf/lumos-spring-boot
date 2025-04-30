package com.lumos.lumosspring.user.dto;

import java.util.List;

public record CreateUserDto(String userId, String username, String name, String lastname,
                            String email, int year, int month, int day,
                            List<String> role, boolean status) { }