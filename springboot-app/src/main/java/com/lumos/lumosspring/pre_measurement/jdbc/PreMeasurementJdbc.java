package com.lumos.lumosspring.pre_measurement.jdbc;

import com.lumos.lumosspring.dto.pre_measurement.*;
import com.lumos.lumosspring.util.ExecutionStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class PreMeasurementJdbc {
    private final NamedParameterJdbcTemplate namedJdbc;

    public PreMeasurementJdbc(NamedParameterJdbcTemplate namedJdbc) {
        this.namedJdbc = namedJdbc;
    }

    public PreMeasurementResponseDTO findById(Long preMeasurementID) {

        return namedJdbc.queryForObject(
                """
                            select p.contract_contract_id,
                                   p.city,
                                   p.type_pre_measurement,
                                   p.total_price,
                                   p.status,
                                   p.step,
                                   au.name || ' ' || au.last_name as complete_name,
                                   p.created_at
                            from pre_measurement p
                            left join app_user au on au.user_id = p.created_by_user_id
                            where p.pre_measurement_id = :preMeasurementID
                        """,
                Map.of("preMeasurementID", preMeasurementID),
                (rs, rowNum) -> {

                    List<PreMeasurementStreetResponseDTO> streets = namedJdbc.query(
                            """
                                        select s.pre_measurement_street_id,
                                               s.address,
                                               s.last_power,
                                               s.latitude,
                                               s.longitude,
                                               street_status
                                        from pre_measurement_street s
                                        where s.pre_measurement_id = :preMeasurementID
                                    """,
                            Map.of("preMeasurementID", preMeasurementID),
                            (rs2, rowNum2) -> {
                                long streetId = rs2.getLong("pre_measurement_street_id");

                                // Subconsulta para pegar os itens associados à rua (street)
                                String streetItemSql = """
                                            select si.pre_measurement_street_item_id,
                                                   si.contract_item_id,
                                                   cri.description,
                                                   cri.name_for_import,
                                                   cri.type ,
                                                   cri.linking ,
                                                   cri.item_dependency,
                                                   si.measured_item_quantity,
                                                   ci.contracted_quantity - ci.quantity_executed as balance,
                                                   si.item_status
                                            from pre_measurement_street_item si
                                            join contract_item ci on si.contract_item_id = ci.contract_item_id
                                            join contract_reference_item cri on cri.contract_reference_item_id = ci.contract_item_reference_id
                                            where si.pre_measurement_street_id = :streetId
                                        """;

                                List<PreMeasurementStreetItemResponseDTO> items = namedJdbc.query(
                                        streetItemSql,
                                        Map.of("streetId", streetId),
                                        (rs3, rowNum3) -> new PreMeasurementStreetItemResponseDTO(
                                                rs3.getLong("pre_measurement_street_item_id"),
                                                rs3.getLong("contract_item_id"),
                                                rs3.getString("description"),
                                                rs3.getString("name_for_import"),
                                                rs3.getString("type"),
                                                rs3.getString("linking"),
                                                rs3.getString("item_dependency"),
                                                rs3.getBigDecimal("measured_item_quantity"),
                                                rs3.getString("item_status")
                                        )
                                );

                                return new PreMeasurementStreetResponseDTO(
                                        rowNum2 + 1,
                                        streetId,
                                        rs2.getString("last_power"),
                                        rs2.getDouble("latitude"),
                                        rs2.getDouble("longitude"),
                                        rs2.getString("address"),
                                        rs2.getString("street_status"),
                                        items
                                );
                            }
                    );

                    // Agora retornamos o PreMeasurementResponseDTO, com as ruas e itens mapeados
                    return new PreMeasurementResponseDTO(
                            preMeasurementID,
                            rs.getLong("contract_contract_id"),
                            rs.getString("city"),
                            rs.getString("type_pre_measurement"),
                            rs.getString("total_price"),
                            rs.getString("status"),
                            rs.getInt("step"),
                            rs.getString("complete_name"),
                            rs.getTimestamp("created_at").toString(),
                            streets
                    );
                }
        );
    }

    public List<ListPreMeasurementResponseDTO> findAllByStatus(String status) {

        return namedJdbc.query(
                """
                            select p.pre_measurement_id,
                               p.contract_contract_id,
                               p.city,
                               p.type_pre_measurement,
                               p.step,
                               au.name || ' ' || au.last_name as complete_name,
                               p.created_at,
                               (select count(*) from pre_measurement_street where pre_measurement_id = p.pre_measurement_id) as street_size,
                               (select count(*) from pre_measurement_street_item where pre_measurement_id = p.pre_measurement_id) as item_size
                            from pre_measurement p
                            left join app_user au on au.user_id = p.created_by_user_id
                            where p.status = :status
                        """,
                Map.of("status", status),
                (rs, _) -> {
                    Instant time = rs.getTimestamp("created_at").toInstant();
                    var timezone = time.atZone(ZoneId.of("America/Sao_Paulo"));
                    var formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
                    var formattedTime = timezone.format(formatter);

                    return new ListPreMeasurementResponseDTO(
                            rs.getLong("pre_measurement_id"),
                            rs.getLong("contract_contract_id"),
                            rs.getString("city"),
                            rs.getString("type_pre_measurement"),
                            rs.getInt("step"),
                            rs.getString("complete_name"),
                            formattedTime,
                            rs.getInt("street_size"),
                            rs.getInt("item_size")
                    );
                }
        );
    }

    public List<CheckBalanceResponse> checkBalance(Long preMeasurementId) {
        return namedJdbc.query(
                """
                         select cri.description,\s
                         sum(pmsi.measured_item_quantity) total_measured,\s
                         sum(ci.contracted_quantity - ci.quantity_executed) - sum(pmsi.measured_item_quantity)  as total_balance,
                         sum(ci.contracted_quantity) as total_contracted_quantity,\s
                         sum(ci.quantity_executed) as total_quantity_executed,\s
                         sum(ci.contracted_quantity - ci.quantity_executed) as total_current_balance
                         from pre_measurement_street_item pmsi\s
                         join contract_item ci on ci.contract_item_id = pmsi.contract_item_id\s
                         join contract_reference_item cri on cri.contract_reference_item_id = ci.contract_item_reference_id\s
                         where pmsi.pre_measurement_id = :preMeasurementId
                         group by ci.contracted_quantity, ci.quantity_executed, cri.description, cri.contract_reference_item_id\s
                        \s""",
                Map.of("preMeasurementId", preMeasurementId),
                (rs, _) -> {
                    return new CheckBalanceResponse(
                            rs.getString("description"),
                            rs.getBigDecimal("total_measured"),
                            rs.getBigDecimal("total_balance"),
                            rs.getBigDecimal("total_contracted_quantity"),
                            rs.getBigDecimal("total_quantity_executed"),
                            rs.getBigDecimal("total_current_balance")
                    );
                }
        );
    }

    public void markAsAvailable(Long preMeasurementId) {
        namedJdbc.update(
                """
                     update pre_measurement set status = :status where pre_measurement_id = :preMeasurementId
                     """,
                Map.of(
                        "preMeasurementId", preMeasurementId,
                        "status", ExecutionStatus.AVAILABLE
                )
        );
    }
}
