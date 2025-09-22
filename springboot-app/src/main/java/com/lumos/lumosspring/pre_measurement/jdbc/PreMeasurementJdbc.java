package com.lumos.lumosspring.pre_measurement.jdbc;

import com.lumos.lumosspring.pre_measurement.dto.response.PreMeasurementResponseDTO;
import com.lumos.lumosspring.pre_measurement.dto.response.PreMeasurementStreetItemResponseDTO;
import com.lumos.lumosspring.pre_measurement.dto.response.PreMeasurementStreetResponseDTO;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PreMeasurementJdbc {
    private final NamedParameterJdbcTemplate namedJdbc;

    public PreMeasurementJdbc(NamedParameterJdbcTemplate namedJdbc) {
        this.namedJdbc = namedJdbc;
    }

    public List<PreMeasurementResponseDTO> findAllByStatus(String status) {

        return namedJdbc.query(
                """
                            select p.pre_measurement_id,
                                   p.contract_contract_id,
                                   p.city,
                                   p.type_pre_measurement,
                                   p.total_price,
                                   p.status,
                                   p.step,
                                   au.name || ' ' || au.last_name as complete_name,
                                   p.created_at
                            from pre_measurement p
                            left join app_user au on au.user_id = p.created_by_user_id
                            where p.status = :status
                        """,
                Map.of("status", status),
                (rs, rowNum) -> {
                    // Para cada PreMeasurement encontrado, fazer uma subconsulta para pegar as ruas (streets) e itens
                    long preMeasurementId = rs.getLong("pre_measurement_id");

                    List<PreMeasurementStreetResponseDTO> streets = namedJdbc.query(
                            """
                                        select s.pre_measurement_street_id,
                                               s.address,
                                               s.last_power,
                                               s.latitude,
                                               s.longitude,
                                               street_status
                                        from pre_measurement_street s
                                        where s.pre_measurement_id = :preMeasurementId
                                    """,
                            Map.of("preMeasurementId", preMeasurementId),
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
                            preMeasurementId,
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

}
