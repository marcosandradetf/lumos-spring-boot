package com.lumos.lumosspring.team.dto;


public record TeamCreate(long idTeam, String teamName, String userId, String UFName, String cityName, String regionName, boolean sel) {
}
