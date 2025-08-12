package com.lumos.lumosspring.dto.team;

import java.util.List;
import java.util.UUID;

public record TeamEdit(long idTeam, List<UUID> userIds) {
}
