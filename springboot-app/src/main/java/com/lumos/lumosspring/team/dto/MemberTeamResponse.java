package com.lumos.lumosspring.team.dto;


import java.util.UUID;

public record MemberTeamResponse(String memberName, UUID userId) {
}
