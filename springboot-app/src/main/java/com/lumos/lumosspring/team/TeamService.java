package com.lumos.lumosspring.team;

import com.lumos.lumosspring.stock.entities.Deposit;
import com.lumos.lumosspring.team.dto.*;
import com.lumos.lumosspring.user.User;
import com.lumos.lumosspring.user.UserRepository;
import com.lumos.lumosspring.user.dto.UpdateUserDto;
import com.lumos.lumosspring.user.dto.UserResponse;
import com.lumos.lumosspring.util.ErrorResponse;
import org.springframework.cache.annotation.Cacheable;
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

    @Cacheable("getAllTeams")
    public ResponseEntity<?> getAll() {
        List<TeamResponse> teamsResponses = teamRepository.findAll().stream()
                .map(team -> new TeamResponse(
                        team.getIdTeam(),
                        team.getTeamName(),
                        new Driver(
                                team.getDriver().getIdUser().toString(),
                                STR."\{team.getDriver().getName()} \{team.getDriver().getLastName()}"
                        ),
                        new Electrician(
                                team.getElectrician().getIdUser().toString(),
                                STR."\{team.getElectrician().getName()} \{team.getElectrician().getLastName()}"
                        ),
                        team.getComplementaryMembers().stream()
                                .map(member -> new Member(
                                        member.getIdUser().toString(),
                                        STR."\{member.getName()} \{member.getLastName()}"
                                ))
                                .toList(), // Convertendo para List<Member>
                        team.getUFName(),
                        team.getCityName(),
                        team.getRegion().getRegionName(),
                        team.getPlateVehicle(),
                        Optional.ofNullable(team.getDeposit())
                                .map(Deposit::getDepositName)
                                .orElseThrow(() -> new IllegalStateException("Equipe sem depósito associado, faça a correção na tela de gerenciamento de equipes!"))
                ))
                .toList(); // Convertendo para List<TeamResponse>

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

            if(teamRepository.findByTeamName(t.teamName()).isPresent()) {
                return ResponseEntity.badRequest().body(new ErrorResponse(STR."Equipe \{t.teamName()} já existe no sistema."));
            }

            var driver = userRepository.findByIdUser(UUID.fromString(t.driver().driverId()));
            var electrician = userRepository.findByIdUser(UUID.fromString(t.electrician().electricianId()));

            if (electrician.isEmpty() || driver.isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Motorista ou eletricista não encontrado no sistema."));
            }

            if (t.electrician().electricianId().equals(t.driver().driverId())){
                return ResponseEntity.badRequest().body(new ErrorResponse(STR."O motorista e o eletricista da equipe \{t.teamName()} não podem ser a mesma pessoa."));
            }

            var hasTeamExists = teamRepository.findByDriver(driver.get());
            if(hasTeamExists.isPresent()){
                return ResponseEntity.badRequest().body(new ErrorResponse(STR."Motorista informado para equipe \{t.teamName()} está cadastrado na equipe \{hasTeamExists.get().getTeamName()}"));
            }

            hasTeamExists = teamRepository.findByElectrician(electrician.get());
            if(hasTeamExists.isPresent()){
                return ResponseEntity.badRequest().body(new ErrorResponse(STR."Eletricista informado para equipe \{t.teamName()}  está cadastrado na equipe \{hasTeamExists.get().getTeamName()}"));
            }

            boolean isDriverInOthers = t.othersMembers().stream()
                    .anyMatch(member -> member.memberId().equals(t.driver().driverId()));

            boolean isElectricianInOthers = t.othersMembers().stream()
                    .anyMatch(member -> member.memberId().equals(t.electrician().electricianId()));

            if (isDriverInOthers || isElectricianInOthers) {
                return ResponseEntity.badRequest().body(new ErrorResponse(STR."O motorista ou o eletricista foram repetidos na seção integrantes complementares da equipe \{t.teamName()}"));
            }

            var otherMembers = new ArrayList<User>();
            for(Member m : t.othersMembers()){
                var member = userRepository.findByIdUser(UUID.fromString(m.memberId()));
                if (member.isEmpty()) {
                    return ResponseEntity.badRequest().body(new ErrorResponse("Algum Colaborador adicional informado não foi encontrado"));
                }
                otherMembers.add(member.get());
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
            newTeam.setDriver(driver.orElse(null));
            newTeam.setElectrician(electrician.orElse(null));
            newTeam.setComplementaryMembers(otherMembers);
            newTeam.setPlateVehicle(t.plate());
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
            if (!t.sel()) {
                continue;
            }

            var driver = userRepository.findByIdUser(UUID.fromString(t.driver().driverId()));
            var electrician = userRepository.findByIdUser(UUID.fromString(t.electrician().electricianId()));

            if (electrician.isEmpty() || driver.isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Motorista ou eletricista não encontrado no sistema."));
            }

            if (t.electrician().electricianId().equals(t.driver().driverId())){
                return ResponseEntity.badRequest().body(new ErrorResponse(STR."O motorista e o eletricista da equipe \{t.teamName()} não podem ser a mesma pessoa."));
            }

            var hasTeamExists = teamRepository.findByDriver(driver.get());
            if(hasTeamExists.isPresent() && hasTeamExists.get().getIdTeam() != t.idTeam()){
                return ResponseEntity.badRequest().body(new ErrorResponse(STR."Motorista informado para equipe \{t.teamName()} está cadastrado na equipe \{hasTeamExists.get().getTeamName()}"));
            }

            hasTeamExists = teamRepository.findByElectrician(electrician.get());
            if(hasTeamExists.isPresent() && hasTeamExists.get().getIdTeam() != t.idTeam()){
                return ResponseEntity.badRequest().body(new ErrorResponse(STR."Eletricista informado para equipe \{t.teamName()} está cadastrado na equipe \{hasTeamExists.get().getTeamName()}"));
            }

            hasTeamExists = teamRepository.findByTeamName(t.teamName());
            if(hasTeamExists.isPresent() && hasTeamExists.get().getIdTeam() != t.idTeam()){
                return ResponseEntity.badRequest().body(new ErrorResponse(STR."Equipe \{t.teamName()} já existe no sistema."));
            }

            boolean isDriverInOthers = t.othersMembers().stream()
                    .anyMatch(member -> member.memberId().equals(t.driver().driverId()));

            boolean isElectricianInOthers = t.othersMembers().stream()
                    .anyMatch(member -> member.memberId().equals(t.electrician().electricianId()));

            if (isDriverInOthers || isElectricianInOthers) {
                return ResponseEntity.badRequest().body(new ErrorResponse(STR."O motorista ou o eletricista foram repetidos na seção integrantes complementares da equipe \{t.teamName()}"));
            }

            var otherMembers = new ArrayList<User>();
            for(Member m : t.othersMembers()){
                var member = userRepository.findByIdUser(UUID.fromString(m.memberId()));
                if (member.isEmpty()) {
                    return ResponseEntity.badRequest().body(new ErrorResponse("Algum Colaborador adicional informado não foi encontrado"));
                }
                otherMembers.add(member.get());
            }

            var region = regionRepository.findRegionByRegionName(t.regionName());
            if (region.isEmpty()) {
                var newRegion = new Region();
                newRegion.setRegionName(t.regionName());
                regionRepository.save(newRegion);
                region = regionRepository.findRegionByRegionName(t.regionName());
            }

            var team = teamRepository.findById(t.idTeam());
            if (team.isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse(STR."O time \{t.teamName()} não foi encontrado no sistema."));
            }
            team.get().setTeamName(t.teamName());
            team.get().setDriver(driver.orElse(null));
            team.get().setElectrician(electrician.orElse(null));
            team.get().setComplementaryMembers(otherMembers);
            team.get().setPlateVehicle(t.plate());
            team.get().setUFName(t.UFName());
            team.get().setCityName(t.cityName());
            team.get().setRegion(region.orElse(null));
            teamRepository.save(team.get());
        }

        return this.getAll();
    }
}
