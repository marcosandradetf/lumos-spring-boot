package com.lumos.lumosspring.team.dto;


import java.util.List;
import java.util.Map;

public record TeamCreate(long idTeam, String teamName, Driver driver, Electrician electrician, List<Member> othersMembers, String UFName, String cityName, String regionName, String plate, boolean sel) {
}
