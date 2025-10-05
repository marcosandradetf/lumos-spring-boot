package com.lumos.lumosspring.user.dto;

import com.lumos.lumosspring.team.dto.TeamResponseForConfirmation;

import java.util.List;

public record OperationalAndTeamsResponse(
        List<OperationalUserResponse> users,
        List<TeamResponseForConfirmation> teams
) {
}
