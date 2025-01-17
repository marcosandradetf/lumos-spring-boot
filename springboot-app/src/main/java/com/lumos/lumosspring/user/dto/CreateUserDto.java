package com.lumos.lumosspring.user.dto;

import java.util.List;

public record CreateUserDto(String username, String name, String lastname,
                            String email, String day, String month, String year,
                            List<String> role, boolean status) { }