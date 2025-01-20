package com.lumos.lumosspring.team;

import com.lumos.lumosspring.team.dto.TeamCreate;
import com.lumos.lumosspring.team.dto.TeamResponse;
import com.lumos.lumosspring.user.User;
import com.lumos.lumosspring.user.UserRepository;
import com.lumos.lumosspring.user.dto.UpdateUserDto;
import com.lumos.lumosspring.user.dto.UserResponse;
import com.lumos.lumosspring.util.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TeamService {
    private final TeamRepository teamRepository;
    private final RegionRepository regionRepository;
    private final UserRepository userRepository;


    public TeamService(TeamRepository teamRepository, RegionRepository regionRepository, UserRepository userRepository) {
        this.teamRepository = teamRepository;
        this.regionRepository = regionRepository;
        this.userRepository = userRepository;
    }

    public ResponseEntity<?> getAll() {
        List<Team> teams = teamRepository.findAll();
        List<TeamResponse> teamsResponses = new ArrayList<>();

        for (Team team : teams) {
            teamsResponses.add(new TeamResponse(
                    team.getIdTeam(),
                    team.getTeamName(),
                    team.getUser().getUsername(),
                    team.getUFName(),
                    team.getCityName(),
                    team.getRegion().getRegionName()
            ));
        }

        return ResponseEntity.ok(teamsResponses);
    }

    public ResponseEntity<?> insertTeams(List<TeamCreate> teams) {
        var hasInvalidUser = teams.stream().noneMatch(u -> u.idTeam() == 0);

        if (hasInvalidUser) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Erro: Foi enviado apenas equipes já existentes no sistema!"));
        }

        for (TeamCreate t : teams) {
            if (t.idTeam() != 0) {
                continue;
            }

            var user = userRepository.findByIdUser(UUID.fromString(t.userId()));

            if (user.isEmpty()) {
                return ResponseEntity.badRequest().body("Usuário informado não encontrado");
            }

            var region = regionRepository.findRegionByRegionName(t.regionName());
            if (region.isEmpty()) {
                var newRegion = new Region();
                newRegion.setRegionName(t.regionName());
                regionRepository.save(newRegion);
                region = regionRepository.findRegionByRegionName(t.regionName());
            }

            var newTeam = new Team();
            newTeam.setTeamName(t.teamName());
            newTeam.setUser(user.get());
            newTeam.setUFName(t.UFName());
            newTeam.setCityName(t.cityName());
            newTeam.setRegion(region.orElse(null));
            teamRepository.save(newTeam);
        }

        return this.getAll();
    }

    public ResponseEntity<?> updateTeams(List<TeamCreate> teams) {
        boolean hasInvalidUser = teams.stream().noneMatch(TeamCreate::sel);

        if (hasInvalidUser) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Erro: Nenhuma equipe selecionado foi enviado."));
        }

        hasInvalidUser = teams.stream().anyMatch(u -> u.idTeam() == 0);
        if (hasInvalidUser) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Erro: Foi enviado uma ou mais equipes sem identificação."));
        }

        for (TeamCreate t : teams) {
            var user = userRepository.findByIdUser(UUID.fromString(t.userId()));

            if (user.isEmpty()) {
                return ResponseEntity.badRequest().body("Usuário informado não encontrado");
            }

            var region = regionRepository.findRegionByRegionName(t.regionName());
            if (region.isEmpty()) {
                var newRegion = new Region();
                newRegion.setRegionName(t.regionName());
                regionRepository.save(newRegion);
                region = regionRepository.findRegionByRegionName(t.regionName());
            }

            var newTeam = new Team();
            newTeam.setTeamName(t.teamName());
            newTeam.setUser(user.get());
            newTeam.setUFName(t.UFName());
            newTeam.setCityName(t.cityName());
            newTeam.setRegion(region.orElse(null));
            teamRepository.save(newTeam);
        }

        return this.getAll();
    }
}
