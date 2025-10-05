package com.lumos.lumosspring.premeasurement.repository.installation;

import com.lumos.lumosspring.premeasurement.dto.installation.StreetsInstallationResponse;
import com.lumos.lumosspring.premeasurement.dto.installation.ItemsInstallationResponse;
import com.lumos.lumosspring.premeasurement.dto.installation.InstallationResponse;
import com.lumos.lumosspring.team.repository.TeamRepository;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class InstallationViewRepository {
    private final NamedParameterJdbcTemplate namedJDBC;
    private final TeamRepository teamRepository;

    public InstallationViewRepository(NamedParameterJdbcTemplate namedJDBC, TeamRepository teamRepository) {
        this.namedJDBC = namedJDBC;
        this.teamRepository = teamRepository;
    }

    public List<InstallationResponse> getInstallations(UUID operatorUUID, String status) {
        var teamId = teamRepository.getCurrentTeamId(operatorUUID).orElse(null);
        if (teamId == null) {
            return Collections.emptyList();
        }

        return namedJDBC.query(
                """
                SELECT p.pre_measurement_id, p.device_pre_measurement_id, c.contractor, p.comment
                FROM pre_measurement p
                JOIN contract c ON c.contract_id = p.contract_contract_id
                WHERE team_id = :teamId
                """,
                Map.of("teamId", teamId),
                (rs, _) -> {
                    Long preMeasurementId = rs.getLong("pre_measurement_id");

                    // Busca as ruas da premedição
                    List<StreetsInstallationResponse> streets = namedJDBC.query(
                            """
                            SELECT s.pre_measurement_street_id,
                                   s.device_pre_measurement_street_id,
                                   s.address,
                                   s.prioritized,
                                   s.latitude,
                                   s.longitude,
                                   s.last_power
                            FROM pre_measurement_street s
                            WHERE s.pre_measurement_id = :preMeasurementId
                            """,
                            Map.of("preMeasurementId", preMeasurementId),
                            (rs2, _) -> {
                                // Busca os materiais/reservas da rua
                                List<ItemsInstallationResponse> reserves = namedJDBC.query(
                                        """
                                        SELECT i.pre_measurement_street_item_id,
                                               i.contract_item_id,
                                               m.material_name,
                                               i.measured_item_quantity,
                                               r.truck_material_stock_id,
                                               ms.request_unit,
                                               COALESCE(m.material_power, m.material_length) AS specs
                                        FROM pre_measurement_street_item i
                                        JOIN material_reservation r 
                                            ON r.contract_item_id = i.contract_item_id 
                                           AND r.pre_measurement_id = i.pre_measurement_id
                                        JOIN material_stock ms 
                                            ON ms.material_id_stock = r.truck_material_stock_id
                                        JOIN material m 
                                            ON m.id_material = ms.material_id
                                        WHERE i.pre_measurement_street_id = :streetId
                                        """,
                                        Map.of("streetId", rs2.getLong("pre_measurement_street_id")),
                                        (rs3, _) -> new ItemsInstallationResponse(
                                                UUID.fromString(rs2.getObject("device_pre_measurement_street_id").toString()),
                                                rs3.getLong("truck_material_stock_id"),
                                                rs3.getLong("contract_item_id"),
                                                rs3.getString("material_name"),
                                                rs3.getBigDecimal("measured_item_quantity"),
                                                rs3.getString("request_unit"),
                                                rs3.getString("specs")
                                        )
                                );

                                return new StreetsInstallationResponse(
                                        rs.getObject("device_pre_measurement_id", UUID.class),
                                        rs2.getObject("device_pre_measurement_street_id", UUID.class),
                                        rs2.getString("address"),
                                        rs2.getBoolean("prioritized"),
                                        rs2.getObject("latitude") != null ? rs2.getDouble("latitude") : null,
                                        rs2.getObject("longitude") != null ? rs2.getDouble("longitude") : null,
                                        rs2.getString("last_power"),
                                        reserves
                                );
                            }
                    );

                    return new InstallationResponse(
                            UUID.fromString(rs.getObject("device_pre_measurement_id").toString()),
                            rs.getString("contractor"),
                            rs.getString("comment"),
                            streets
                    );
                }
        );
    }
}
