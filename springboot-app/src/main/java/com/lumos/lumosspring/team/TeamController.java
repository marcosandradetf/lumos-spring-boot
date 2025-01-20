package com.lumos.lumosspring.team;

import com.lumos.lumosspring.team.dto.TeamCreate;
import com.lumos.lumosspring.team.dto.TeamResponse;
import com.lumos.lumosspring.user.UserService;
import com.lumos.lumosspring.user.dto.UserResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
public class TeamController {
    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping("/get-teams")
    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN', 'SCOPE_ANALISTA')")
    public ResponseEntity<?> findAll() {
        return teamService.getAll();
    }

    @GetMapping("/post-teams")
    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN', 'SCOPE_ANALISTA')")
    public ResponseEntity<?> postAll(@RequestBody List<TeamCreate> teams) {
        return teamService.insertTeams(teams);
    }

    @GetMapping("/update-teams")
    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN', 'SCOPE_ANALISTA')")
    public ResponseEntity<?> updateAll(@RequestBody List<TeamCreate> teams) {
        return teamService.updateTeams(teams);
    }
}
