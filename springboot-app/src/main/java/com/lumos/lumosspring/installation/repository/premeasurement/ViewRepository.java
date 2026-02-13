package com.lumos.lumosspring.installation.repository.premeasurement;

import com.lumos.lumosspring.installation.dto.premeasurement.StreetsInstallationResponse;
import com.lumos.lumosspring.installation.dto.premeasurement.ItemsInstallationResponse;
import com.lumos.lumosspring.installation.dto.premeasurement.InstallationResponse;
import com.lumos.lumosspring.s3.service.S3Service;
import com.lumos.lumosspring.team.repository.TeamRepository;
import com.lumos.lumosspring.util.Utils;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class ViewRepository {
    private final NamedParameterJdbcTemplate namedJDBC;
    private final TeamRepository teamRepository;
    private final S3Service s3Service;

    public ViewRepository(NamedParameterJdbcTemplate namedJDBC, TeamRepository teamRepository, S3Service s3Service) {
        this.namedJDBC = namedJDBC;
        this.teamRepository = teamRepository;
        this.s3Service = s3Service;
    }

    public List<InstallationResponse> getInstallations(UUID userId, String status, Long teamId) {
        if (teamId == null && userId != null) {
            teamId = teamRepository.getCurrentTeamId(userId).orElse(null);
            if (teamId == null) {
                return Collections.emptyList();
            }
        }

        return namedJDBC.query(
                """
                        SELECT p.pre_measurement_id, p.device_pre_measurement_id, c.contract_id, c.contractor, p.comment
                        FROM pre_measurement p
                        JOIN contract c ON c.contract_id = p.contract_contract_id
                        WHERE team_id = :teamId and p.status = :status
                        """,
                Map.of("teamId", teamId, "status", status),
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
                                           s.last_power,
                                           s.pre_measurement_photo_uri
                                    FROM pre_measurement_street s
                                    WHERE s.pre_measurement_id = :preMeasurementId
                                    """,
                            Map.of("preMeasurementId", preMeasurementId),
                            (rs2, _) -> {
                                // Busca os materiais/reservas da rua
                                List<ItemsInstallationResponse> items = namedJDBC.query(
                                        """
                                                SELECT i.pre_measurement_street_item_id,
                                                       i.contract_item_id,
                                                       m.material_name,
                                                       i.measured_item_quantity,
                                                       r.truck_material_stock_id,
                                                       ms.request_unit,
                                                       ci.contracted_quantity - ci.quantity_executed as current_balance,
                                                       COALESCE(cri.name_for_import, cri.description) as item_name
                                                FROM pre_measurement_street_item i
                                                JOIN material_reservation r
                                                    ON r.contract_item_id = i.contract_item_id
                                                   AND r.pre_measurement_id = i.pre_measurement_id
                                                JOIN material_stock ms
                                                    ON ms.material_id_stock = r.truck_material_stock_id
                                                JOIN material m
                                                    ON m.id_material = ms.material_id
                                                JOIN contract_item ci
                                                    ON ci.contract_item_id = i.contract_item_id
                                                JOIN contract_reference_item cri
                                                    ON cri.contract_reference_item_id = ci.contract_item_reference_id
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
                                                rs3.getBigDecimal("current_balance"),
                                                rs3.getString("item_name")
                                        )
                                );

                                var publicUrl = s3Service.getPublicUrl(Utils.getCurrentBucket(), rs2.getString("pre_measurement_photo_uri"), 2 * 24 * 60 * 60); // 2 dias
                                return new StreetsInstallationResponse(
                                        rs.getObject("device_pre_measurement_id", UUID.class),
                                        rs2.getObject("device_pre_measurement_street_id", UUID.class),
                                        rs2.getString("address"),
                                        rs2.getBoolean("prioritized"),
                                        rs2.getObject("latitude") != null ? rs2.getDouble("latitude") : null,
                                        rs2.getObject("longitude") != null ? rs2.getDouble("longitude") : null,
                                        rs2.getString("last_power"),
                                        publicUrl.getUrl(),
                                        publicUrl.getExpiresAt(),
                                        rs2.getString("pre_measurement_photo_uri"),
                                        items
                                );
                            }
                    );

                    return new InstallationResponse(
                            UUID.fromString(rs.getObject("device_pre_measurement_id").toString()),
                            rs.getLong("contract_id"),
                            rs.getString("contractor"),
                            rs.getString("comment"),
                            streets
                    );
                }
        );
    }
}
