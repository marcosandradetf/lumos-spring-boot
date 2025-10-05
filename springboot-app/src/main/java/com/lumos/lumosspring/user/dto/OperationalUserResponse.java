package com.lumos.lumosspring.user.dto;

import java.util.UUID;

public record OperationalUserResponse(
        UUID userId, String completeName
) {
}
