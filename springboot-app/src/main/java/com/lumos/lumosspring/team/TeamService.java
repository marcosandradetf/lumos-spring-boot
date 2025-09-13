package com.lumos.lumosspring.team;

import com.lumos.lumosspring.dto.team.*;
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
import org.springframework.dao.DataIntegrityViolationException;
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
            @CacheEvict(cacheNames = "getAllTeams", allEntries = true),
            @CacheEvict(cacheNames = "getAllDeposits", allEntries = true),
    })
    @Transactional
    public void insertTeam(TeamCreate t) {
        var regions = regionRepository.findRegionByRegionName(t.regionName());
        Region region;
        if (regions.isEmpty()) {
            region = regionRepository.save(new Region(null, t.regionName()));
        } else {
            region = regions.getFirst();
        }

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

            namedParameterJdbcTemplate.update(
                    """
                            insert into material_stock (
                                buy_unit,
                                cost_per_item,
                                cost_price,
                                inactive,
                                request_unit,
                                stock_available,
                                stock_quantity,
                                company_id,
                                deposit_id,
                                material_id
                            )
                            select
                                'UN',
                                null as cost_per_item,    \s
                                null as cost_price,       \s
                                false as inactive,       \s
                                'UN',
                                0 as stock_available,     \s
                                0 as stock_quantity,      \s
                                :companyId,
                                :depositId,               \s
                                m.id_material\s
                            from material m;
                            """,
                    Map.of(
                            "depositId", deposit.getIdDeposit(),
                            "companyId", deposit.getCompanyId()
                    )
            );

            newTeam.setDepositId(deposit.getIdDeposit());
            newTeam = teamRepository.save(newTeam);

            updateTeam(
                    new TeamEdit(
                            newTeam.getIdTeam(),
                            t.memberIds()
                    )
            );
        } catch (Exception ex) {
            if (ex.getCause().toString().contains("duplicate key")) {
                throw new Utils.BusinessException(
                        "Não é possível salvar: já existe uma equipe com esse nome ou placa de veículo."
                );
            }

            throw new Utils.BusinessException("Erro ao atualizar o depósito: " + ex.getMessage());
        }


        this.getAll();
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "getAllTeams", allEntries = true),
            @CacheEvict(cacheNames = "getAllDeposits", allEntries = true),
    })
    public ResponseEntity<?> updateTeams(List<TeamCreate> teams) {
        boolean hasInvalidUser = teams.stream().noneMatch(TeamCreate::sel);

        if (hasInvalidUser) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Erro: Nenhuma equipe selecionado foi enviado."));
        }

        for (TeamCreate t : teams) {
            if (!t.sel()) {
                continue;
            }

            if (t.idTeam() == 0) {
                this.insertTeam(t);
                continue;
            }

            var team = teamRepository.findById(t.idTeam())
                    .orElseThrow(() -> new Utils.BusinessException(
                            "O time %s não foi encontrado no sistema.".formatted(t.teamName())
                    ));

            var regions = regionRepository.findRegionByRegionName(t.regionName());
            Region region;
            if (regions.isEmpty()) {
                region = regionRepository.save(new Region(null, t.regionName()));
            } else {
                region = regions.getFirst();
            }

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

                if (t.memberIds() != null) {
                    updateTeam(
                            new TeamEdit(
                                    t.idTeam(),
                                    t.memberIds()
                            )
                    );
                }

            } catch (Exception ex) {
                if (ex.getMessage() != null && ex.getCause().toString().contains("duplicate key")) {
                    throw new Utils.BusinessException("Já existe uma equipe ou caminhão com esse nome ou placa.");
                }
                throw new Utils.BusinessException("Erro de integridade: " + ex.getCause());
            }

        }

        return this.getAll();
    }

    public ResponseEntity<?> updateTeam(TeamEdit team) {
        teamQueryRepository.renewTeam(team.idTeam(), team.userIds());

        return ResponseEntity.noContent().build();
    }
}
