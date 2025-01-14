package com.lumos.lumosspring.user.dto;

import java.util.List;

public record UserResponse(String userId, String username, String name, String lastName, String email, List<String> role, String dateOfBirth) {
}
