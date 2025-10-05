package com.lumos.lumosspring.team.dto;

import java.util.List;
import java.util.UUID;

public record TeamEdit(long idTeam, List<UUID> userIds) {
}
