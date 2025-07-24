package com.lumos.lumosspring.team.dto;


public record TeamCreate(long idTeam, String teamName, Driver driver, Electrician electrician, String UFName, String cityName, String regionName, String plate, boolean sel) {
}
