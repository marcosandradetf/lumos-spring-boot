package com.lumos.lumosspring.team.dto;


import com.lumos.lumosspring.user.dto.UserResponse;

import java.util.List;

public record TeamResponse(long idTeam, String teamName, Driver driver, Electrician electrician, List<Member> othersMembers, String UFName, String cityName, String regionName, String plate) {
}
