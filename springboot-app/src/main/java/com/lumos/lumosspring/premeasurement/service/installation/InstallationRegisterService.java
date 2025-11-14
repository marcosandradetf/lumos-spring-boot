package com.lumos.lumosspring.premeasurement.service.installation;

import com.lumos.lumosspring.minio.service.MinioService;
import com.lumos.lumosspring.premeasurement.dto.installation.InstallationItemRequest;
import com.lumos.lumosspring.premeasurement.dto.installation.InstallationRequest;
import com.lumos.lumosspring.premeasurement.dto.installation.InstallationStreetRequest;
import com.lumos.lumosspring.util.ExecutionStatus;
import com.lumos.lumosspring.util.JdbcUtil;
import com.lumos.lumosspring.util.Utils;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class InstallationRegisterService {
    private final NamedParameterJdbcTemplate namedJdbc;
    private final MinioService minioService;

    public InstallationRegisterService(NamedParameterJdbcTemplate namedJdbc, MinioService minioService) {
        this.namedJdbc = namedJdbc;
        this.minioService = minioService;
    }

    @Transactional
    public ResponseEntity<?> saveStreetInstallation(MultipartFile photo, InstallationStreetRequest installationReq) {
        if (installationReq == null) {
            throw new Utils.BusinessException("payload vazio.");
        }

        var installation = JdbcUtil.INSTANCE.getSingleRow(
                namedJdbc,
                """
                            SELECT p.pre_measurement_id, p.status, p.city
                            FROM pre_measurement p
                            JOIN pre_measurement_street s on s.pre_measurement_id = p.pre_measurement_id
                            WHERE s.device_pre_measurement_street_id = :streetId
                        """,
                Map.of("streetId", installationReq.getStreetId())
        );

        if (installation == null) {
            String message = new StringBuilder()
                    .append("Instalação com ID ")
                    .append(installationReq.getStreetId())
                    .append(" não encontrada")
                    .toString();
            throw new Utils.BusinessException(message);
        }

        if (!Objects.equals(installation.get("status").toString(), ExecutionStatus.AVAILABLE_EXECUTION)) {
            return ResponseEntity.noContent().build();
        }

        Long preMeasurementID = ((Number) installation.get("pre_measurement_id")).longValue();

        String sql;
        for (InstallationItemRequest r : installationReq.getItems()) {
            String materialName = r.getMaterialName().toLowerCase(Locale.ROOT);
            String hasService;

            if (materialName.contains("led")) {
                hasService = "led";
            } else if (materialName.contains("braço")) {
                hasService = "braço";
            } else {
                hasService = null;
            }

            Map<String, Object> params = new HashMap<>(Map.of(
                    "quantityExecuted", r.getQuantityExecuted(),
                    "contractItemId", r.getContractItemId()
            ));

            if (hasService != null) {
                params.put("dependency", hasService);
                params.put("preMeasurementId", preMeasurementID);

                namedJdbc.update(
                        """
                                WITH to_update AS (
                                    SELECT ci.contract_item_id
                                    FROM contract_item ci
                                    WHERE ci.contract_item_id = :contractItemId
                                
                                    UNION ALL
                                
                                    SELECT ci.contract_item_id
                                    FROM contract_item ci
                                    JOIN contract_reference_item cri ON cri.contract_reference_item_id = ci.contract_item_reference_id
                                    JOIN pre_measurement_street_item si ON si.contract_item_id = ci.contract_item_id
                                    WHERE lower(cri.item_dependency) = :dependency
                                      AND lower(cri.type) IN ('projeto', 'serviço')
                                      AND si.pre_measurement_id = :preMeasurementId
                                )
                                UPDATE contract_item ci
                                SET quantity_executed = quantity_executed + :quantityExecuted
                                FROM to_update tu
                                WHERE ci.contract_item_id = tu.contract_item_id
                                """,
                        params
                );

            } else {
                // Use update porque não há retorno
                namedJdbc.update(
                        """
                                        UPDATE contract_item
                                        SET quantity_executed = quantity_executed + :quantityExecuted
                                        WHERE contract_item_id = :contractItemId
                                """,
                        params
                );
            }

            namedJdbc.update(
                    """
                            UPDATE material_reservation
                            SET quantity_completed = quantity_completed + :quantityCompleted
                            WHERE truck_material_stock_id = :materialStockId
                                AND pre_measurement_id = :preMeasurementId
                            """,
                    Map.of(
                            "quantityCompleted", r.getQuantityExecuted(),
                            "preMeasurementId", preMeasurementID,
                            "materialStockId", r.getTruckMaterialStockId()
                    )
            );

            namedJdbc.update(
                    """
                            UPDATE material_stock
                            SET stock_quantity = stock_quantity - :quantityCompleted,
                                stock_available = stock_available - :quantityCompleted
                            WHERE material_id_stock = :materialStockId
                            """,
                    Map.of(
                            "quantityCompleted", r.getQuantityExecuted(),
                            "materialStockId", r.getTruckMaterialStockId()
                    )
            );

        }

        if (photo != null) {
            String city = ((String) installation.get("city"));
            String folder = "photos";
            if (city != null) folder = "photos/$city";

            String fileUri = minioService.uploadFile(photo, Utils.INSTANCE.getCurrentBucket(), folder, "execution");

            sql = """
                        UPDATE pre_measurement_street
                        SET execution_photo_uri = :fileUri, street_status = :streetStatus
                        WHERE device_pre_measurement_street_id = :streetId
                    """;

            namedJdbc.update(
                    sql,
                    Map.of(
                            "fileUri", fileUri,
                            "streetStatus", ExecutionStatus.FINISHED,
                            "streetId", installationReq.getStreetId()
                    )
            );
        }

        return ResponseEntity.noContent().build();
    }

    @Transactional
    public ResponseEntity<?> saveInstallation(MultipartFile photo, InstallationRequest installationReq) {

        if (photo != null) {
            String city = ((String) execution.get("city"));
            String folder = "photos";
            if (city != null) folder = "photos/$city";

            String fileUri = minioService.uploadFile(photo, Utils.INSTANCE.getCurrentBucket(), folder, "execution");


            namedJdbc.update(
                    """
                        UPDATE pre_measurement_street
                        SET execution_photo_uri = :fileUri, street_status = :streetStatus
                        WHERE device_pre_measurement_street_id = :streetId
                    """,
                    Map.of(
                            "fileUri", fileUri,
                            "streetStatus", ExecutionStatus.FINISHED,
                            "streetId", installationReq.getStreetId()
                    )
            );
        }

        return ResponseEntity.noContent().build();
    }


}
