package com.lumos.lumosspring.team.dto;


public record TeamResponseForConfirmation(
        long teamId,
        String depositName,
        String teamName,
        String plateVehicle
) {
}
