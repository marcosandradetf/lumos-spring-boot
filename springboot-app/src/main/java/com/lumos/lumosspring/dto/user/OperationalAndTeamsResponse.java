package com.lumos.lumosspring.dto.user;

import com.lumos.lumosspring.dto.team.TeamResponseForConfirmation;

import java.util.List;

public record OperationalAndTeamsResponse(
        List<OperationalUserResponse> users,
        List<TeamResponseForConfirmation> teams
) {
}
