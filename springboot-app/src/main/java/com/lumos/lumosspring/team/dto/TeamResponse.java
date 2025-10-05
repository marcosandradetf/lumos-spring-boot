package com.lumos.lumosspring.team.dto;


import java.util.List;
import java.util.UUID;

public record TeamResponse(long idTeam, String teamName, String UFName, String cityName, String regionName, String plate, String depositName,
                           List<UUID> memberIds,
                           List<String> memberNames
) {
}
