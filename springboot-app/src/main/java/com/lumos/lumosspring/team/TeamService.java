package com.lumos.lumosspring.team;

import com.lumos.lumosspring.stock.entities.Deposit;
import com.lumos.lumosspring.stock.repository.DepositRepository;
import com.lumos.lumosspring.team.dto.*;
import com.lumos.lumosspring.team.entities.Region;
import com.lumos.lumosspring.team.entities.Team;
import com.lumos.lumosspring.team.repository.RegionRepository;
import com.lumos.lumosspring.team.repository.TeamRepository;
import com.lumos.lumosspring.user.UserRepository;
import com.lumos.lumosspring.util.ErrorResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.StreamSupport;

@Service
public class TeamService {
    private final TeamRepository teamRepository;
    private final RegionRepository regionRepository;
    private final UserRepository userRepository;
    private final DepositRepository depositRepository;


    public TeamService(TeamRepository teamRepository, RegionRepository regionRepository, UserRepository userRepository,
                       DepositRepository depositRepository) {
        this.teamRepository = teamRepository;
        this.regionRepository = regionRepository;
        this.userRepository = userRepository;
        this.depositRepository = depositRepository;
    }

    @Cacheable("getAllTeams")
    public ResponseEntity<?> getAll() {
        var teamsIterable = teamRepository.findAll();

        List<TeamResponse> teamsResponses = StreamSupport.stream(teamsIterable.spliterator(), false)
                .map(team -> {
                    // Buscar driver pelo driverId
                    var driverOpt = userRepository.findById(team.getDriverId());
                    Driver driver = driverOpt
                            .map(d -> new Driver(d.getUserId().toString(), d.getCompletedName()))
                            .orElse(null); // Ou lance erro se não encontrar

                    // Buscar eletricista pelo electricianId
                    var electricianOpt = userRepository.findById(team.getElectricianId());
                    Electrician electrician = electricianOpt
                            .map(e -> new Electrician(e.getUserId().toString(), e.getCompletedName()))
                            .orElse(null); // Ou lance erro se não encontrar

                    // Buscar os membros complementares explicitamente
//                    List<Member> complementaryMembers = team.getComplementaryMembers().stream()
//                            .map(member -> {
//                                // buscar cada membro pelo userId
//                                var memberOpt = teamComplementaryMemberRepository.findAllby(member.getUserId());
//                                return memberOpt
//                                        .map(m -> new Member(m.getUserId().toString(), m.getName() + " " + m.getLastName()))
//                                        .orElseGet(() -> new Member(member.getUserId().toString(), "Nome não encontrado"));
//                            })
//                            .toList();

                    // Buscar o depósito usando o depositId
                    var depositOpt = depositRepository.findById(team.getDepositId());
                    String depositName = depositOpt
                            .map(Deposit::getDepositName)
                            .orElseThrow(() -> new IllegalStateException("Equipe sem depósito associado, faça a correção na tela de gerenciamento de equipes!"));
                    var region = regionRepository.findById(team.getRegion()).orElse(null);

                    return new TeamResponse(
                            team.getIdTeam(),
                            team.getTeamName(),
                            driver,
                            electrician,
                            team.getUFName(),
                            team.getCityName(),
                            region.getRegionName(),
                            team.getPlateVehicle(),
                            depositName // Aqui a string do nome do depósito
                    );
                })
                .sorted(Comparator.comparing(TeamResponse::teamName))
                .toList();

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

            var driver = userRepository.findByUserId(UUID.fromString(t.driver().driverId()));
            var electrician = userRepository.findByUserId(UUID.fromString(t.electrician().electricianId()));

            if (electrician.isEmpty() || driver.isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Motorista ou eletricista não encontrado no sistema."));
            }

            if (t.electrician().electricianId().equals(t.driver().driverId())){
                return ResponseEntity.badRequest().body(new ErrorResponse(STR."O motorista e o eletricista da equipe \{t.teamName()} não podem ser a mesma pessoa."));
            }

            var hasTeamExists = teamRepository.findByDriverId(driver.get().getUserId());
            if(hasTeamExists.isPresent()){
                return ResponseEntity.badRequest().body(new ErrorResponse(STR."Motorista informado para equipe \{t.teamName()} está cadastrado na equipe \{hasTeamExists.get().getTeamName()}"));
            }

            hasTeamExists = teamRepository.findByElectricianId(electrician.get().getUserId());
            if(hasTeamExists.isPresent()){
                return ResponseEntity.badRequest().body(new ErrorResponse(STR."Eletricista informado para equipe \{t.teamName()}  está cadastrado na equipe \{hasTeamExists.get().getTeamName()}"));
            }

            var region = regionRepository.findRegionByRegionName(t.regionName());
            if (region.isEmpty()) {
                var newRegion = new Region(
                        null,
                        t.regionName()
                );
                regionRepository.save(newRegion);
                region = regionRepository.findRegionByRegionName(t.regionName());
            }

            var newTeam = new Team();
            newTeam.setTeamName(t.teamName());
            newTeam.setDriverId(driver.orElse(null).getUserId());
            newTeam.setElectricianId(electrician.orElse(null).getUserId());

            newTeam.setPlateVehicle(t.plate());
            newTeam.setUFName(t.UFName());
            newTeam.setCityName(t.cityName());
            newTeam.setRegion(region.orElse(null).getRegionId());
            newTeam =  teamRepository.save(newTeam);
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

            var driver = userRepository.findByUserId(UUID.fromString(t.driver().driverId()));
            var electrician = userRepository.findByUserId(UUID.fromString(t.electrician().electricianId()));

            if (electrician.isEmpty() || driver.isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Motorista ou eletricista não encontrado no sistema."));
            }

            if (t.electrician().electricianId().equals(t.driver().driverId())){
                return ResponseEntity.badRequest().body(new ErrorResponse(STR."O motorista e o eletricista da equipe \{t.teamName()} não podem ser a mesma pessoa."));
            }

            var hasTeamExists = teamRepository.findByDriverId(driver.get().getUserId());
            if(hasTeamExists.isPresent() && hasTeamExists.get().getIdTeam() != t.idTeam()){
                return ResponseEntity.badRequest().body(new ErrorResponse(STR."Motorista informado para equipe \{t.teamName()} está cadastrado na equipe \{hasTeamExists.get().getTeamName()}"));
            }

            hasTeamExists = teamRepository.findByElectricianId(electrician.get().getUserId());
            if(hasTeamExists.isPresent() && hasTeamExists.get().getIdTeam() != t.idTeam()){
                return ResponseEntity.badRequest().body(new ErrorResponse(STR."Eletricista informado para equipe \{t.teamName()} está cadastrado na equipe \{hasTeamExists.get().getTeamName()}"));
            }

            hasTeamExists = teamRepository.findByTeamName(t.teamName());
            if(hasTeamExists.isPresent() && hasTeamExists.get().getIdTeam() != t.idTeam()){
                return ResponseEntity.badRequest().body(new ErrorResponse(STR."Equipe \{t.teamName()} já existe no sistema."));
            }


            var region = regionRepository.findRegionByRegionName(t.regionName());
            if (region.isEmpty()) {
                var newRegion = new Region(
                        null,
                        t.regionName()
                );
                regionRepository.save(newRegion);
                region = regionRepository.findRegionByRegionName(t.regionName());
            }

            var team = teamRepository.findById(t.idTeam());
            if (team.isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse(STR."O time \{t.teamName()} não foi encontrado no sistema."));
            }

            team.get().setTeamName(t.teamName());
            team.get().setDriverId(driver.orElse(null).getUserId());
            team.get().setElectricianId(electrician.orElse(null).getUserId());

            team.get().setPlateVehicle(t.plate());
            team.get().setUFName(t.UFName());
            team.get().setCityName(t.cityName());
            team.get().setRegion(region.orElse(null).getRegionId());
            teamRepository.save(team.get());
        }

        return this.getAll();
    }
}
