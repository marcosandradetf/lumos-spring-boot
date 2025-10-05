package com.lumos.lumosspring.team.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TeamCreate(long idTeam, String teamName, List<UUID> memberIds, String UFName, String cityName, String regionName, String plate, boolean sel) {
}

