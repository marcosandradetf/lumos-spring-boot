package com.lumos.lumosspring.team.controller;

import com.lumos.lumosspring.team.dto.TeamCreate;
import com.lumos.lumosspring.team.dto.TeamEdit;
import com.lumos.lumosspring.team.service.TeamService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TeamController {
    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping("/teams/get-teams")
    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN', 'SCOPE_ANALISTA')")
    public ResponseEntity<?> findAll() {
        return teamService.getAll();
    }

    @PostMapping("/teams/update-teams")
    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN', 'SCOPE_ANALISTA')")
    public ResponseEntity<?> updateAll(@RequestBody List<TeamCreate> teams) {
        return teamService.updateTeams(teams);
    }


    @PostMapping("/mobile/teams/update-team")
    public ResponseEntity<?> updateTeam(@RequestBody TeamEdit team) {
        return teamService.updateTeam(team);
    }

    @PostMapping("/teams/send-stock-notification")
    public ResponseEntity<?> sendStockNotification(
            @RequestParam String description,
            @RequestParam String notificationCode,
            @RequestParam String materialName) {
        return teamService.sendStockNotification(description, notificationCode, materialName);
    }

}
