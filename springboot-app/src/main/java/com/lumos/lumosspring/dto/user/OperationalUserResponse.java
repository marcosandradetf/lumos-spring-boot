package com.lumos.lumosspring.dto.user;

import java.util.UUID;

public record OperationalUserResponse(
        UUID userId, String completeName
) {
}
