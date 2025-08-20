package com.lumos.lumosspring.dto.team;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TeamCreate(long idTeam, String teamName, List<UUID> membersIds, String UFName, String cityName, String regionName, String plate, boolean sel) {
}

