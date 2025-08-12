package com.lumos.lumosspring.dto.team;


public record TeamResponse(long idTeam, String teamName, Driver driver, Electrician electrician, String UFName, String cityName, String regionName, String plate, String depositName) {
}
