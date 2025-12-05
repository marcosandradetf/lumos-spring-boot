package com.lumos.lumosspring.team.dto;

import java.util.UUID;

public record TeamResponseForConfirmation(
        long teamId,
        String depositName,
        String teamName,
        String plateVehicle,
        UUID notificationTopic
) {
}
