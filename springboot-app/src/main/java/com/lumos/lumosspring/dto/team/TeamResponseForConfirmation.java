package com.lumos.lumosspring.dto.team;


public record TeamResponseForConfirmation(
        long teamId,
        String depositName,
        String teamName,
        String plateVehicle
) {
}
