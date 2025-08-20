package com.lumos.lumosspring.team;

import com.lumos.lumosspring.dto.team.*;
import com.lumos.lumosspring.notifications.service.NotificationService;
import com.lumos.lumosspring.stock.entities.Deposit;
import com.lumos.lumosspring.stock.repository.DepositRepository;
import com.lumos.lumosspring.team.entities.Region;
import com.lumos.lumosspring.team.entities.Team;
import com.lumos.lumosspring.team.repository.RegionRepository;
import com.lumos.lumosspring.team.repository.TeamQueryRepository;
import com.lumos.lumosspring.team.repository.TeamRepository;
import com.lumos.lumosspring.util.ErrorResponse;
import com.lumos.lumosspring.util.Utils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.StreamSupport;

@Service
public class TeamService {
    private final TeamRepository teamRepository;
    private final RegionRepository regionRepository;
    private final DepositRepository depositRepository;
    private final TeamQueryRepository teamQueryRepository;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public TeamService(TeamRepository teamRepository,
                       RegionRepository regionRepository,
                       DepositRepository depositRepository,
                       TeamQueryRepository teamQueryRepository, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {

        this.teamRepository = teamRepository;
        this.regionRepository = regionRepository;
        this.depositRepository = depositRepository;
        this.teamQueryRepository = teamQueryRepository;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @Cacheable("getAllTeams")
    public ResponseEntity<?> getAll() {
        var teamsIterable = teamRepository.findAll();

        List<TeamResponse> teamsResponses = StreamSupport.stream(teamsIterable.spliterator(), false)
                .map(team -> {

                    // Buscar o depósito usando o depositId
                    var depositOpt = depositRepository.findById(team.getDepositId());
                    String depositName = depositOpt
                            .map(Deposit::getDepositName)
                            .orElseThrow(() -> new IllegalStateException("Equipe sem depósito associado, faça a correção na tela de gerenciamento de equipes!"));
                    var region = regionRepository.findById(team.getRegion()).orElse(null);
                    var members = teamRepository.getMembers(team.getIdTeam());

                    var memberIds = members.stream().map(MemberTeamResponse::userId).toList();
                    var memberNames = members.stream().map(MemberTeamResponse::memberName).toList();

                    return new TeamResponse(
                            team.getIdTeam(),
                            team.getTeamName(),
                            team.getUFName(),
                            team.getCityName(),
                            region.getRegionName(),
                            team.getPlateVehicle(),
                            depositName,
                            memberIds,
                            memberNames
                    );
                })
                .sorted(Comparator.comparing(TeamResponse::teamName))
                .toList();

        return ResponseEntity.ok(teamsResponses);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "getAll", allEntries = true),
    })
    @Transactional
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

            var region = regionRepository.findRegionByRegionName(t.regionName())
                    .orElseGet(() -> regionRepository.save(new Region(null, t.regionName())));

            var newTeam = new Team();
            newTeam.setTeamName(t.teamName());

            newTeam.setPlateVehicle(t.plate());
            newTeam.setUFName(t.UFName());
            newTeam.setCityName(t.cityName());
            newTeam.setRegion(region.getRegionId());

            var deposit = new Deposit();

            deposit.setRegion(region.getRegionId());
            deposit.setDepositCity(t.cityName());
            deposit.setDepositName(t.teamName());
            deposit.setTruck(true);
            deposit.setCompanyId(1L);

            try {
                deposit = depositRepository.save(deposit);
                newTeam.setDepositId(deposit.getIdDeposit());
                newTeam = teamRepository.save(newTeam);

                updateTeam(
                        new TeamEdit(
                                newTeam.getIdTeam(),
                                t.membersIds()
                        )
                );
            } catch (DuplicateKeyException ex) {
                throw new Utils.BusinessException(
                        "Não é possível salvar: já existe uma equipe com esse nome e placa de veículo."
                );
            } catch (DataAccessException ex) {
                // pega erros do JDBC sem ser duplicidade
                throw new Utils.BusinessException("Erro ao atualizar o depósito: " + ex.getMessage());
            }
        }

        return this.getAll();
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "getAll", allEntries = true),
    })
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

            var region = regionRepository.findRegionByRegionName(t.regionName())
                    .orElseGet(() -> regionRepository.save(new Region(null, t.regionName())));


            var team = teamRepository.findById(t.idTeam())
                    .orElseThrow(() -> new Utils.BusinessException(
                            "O time %s não foi encontrado no sistema.".formatted(t.teamName())
                    ));

            team.setTeamName(t.teamName());
            team.setPlateVehicle(t.plate());
            team.setUFName(t.UFName());
            team.setCityName(t.cityName());
            team.setRegion(region.getRegionId());

            var depositId = team.getDepositId();


            try {
                teamRepository.save(team);
                namedParameterJdbcTemplate.update("""
                            UPDATE deposit
                            SET deposit_name = :teamName
                            WHERE id_deposit = :depositId
                        """, Map.of("teamName", t.teamName(), "depositId", depositId));

                updateTeam(
                        new TeamEdit(
                                t.idTeam(),
                                t.membersIds()
                        )
                );
            } catch (DuplicateKeyException ex) {
                throw new Utils.BusinessException(
                        "Não é possível salvar: já existe uma equipe com esse nome e placa de veículo."
                );
            } catch (DataAccessException ex) {
                // pega erros do JDBC sem ser duplicidade
                throw new Utils.BusinessException("Erro ao atualizar o depósito: " + ex.getMessage());
            }

        }

        return this.getAll();
    }

    public ResponseEntity<?> updateTeam(TeamEdit team) {
        teamQueryRepository.renewTeam(team.idTeam(), team.userIds());

        return ResponseEntity.noContent().build();
    }
}
