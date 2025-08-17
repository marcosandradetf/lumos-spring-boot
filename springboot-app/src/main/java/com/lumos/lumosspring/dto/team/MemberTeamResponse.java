package com.lumos.lumosspring.dto.team;


import java.util.UUID;

public record MemberTeamResponse(String memberName, UUID userId) {
}
