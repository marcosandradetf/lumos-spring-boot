package com.lumos.lumosspring.premeasurement.repository.installation;

import com.lumos.lumosspring.premeasurement.model.PreMeasurement;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface PreMeasurementInstallationRepository extends CrudRepository<PreMeasurement, Long> {
    record PreMeasurementRow(Long preMeasurementId, String status, String description, String preMeasurementPhotoUri) {}

    @Query(
            """
                SELECT p.pre_measurement_id, p.status, 'Etapa ' || p.step || ' - ' || p.city as description, s.pre_measurement_photo_uri
                FROM pre_measurement p
                JOIN pre_measurement_street s on s.pre_measurement_id = p.pre_measurement_id
                WHERE s.device_pre_measurement_street_id = :streetId
            """
    )
    PreMeasurementRow getInstallationByDeviceStreetId(UUID streetId);

    @Query(
            """
                SELECT p.pre_measurement_id, p.status, 'Etapa ' || p.step || ' - ' || p.city as description, null
                FROM pre_measurement p
                where p.device_pre_measurement_id = :installationId
            """
    )
    PreMeasurementRow getInstallationByDeviceInstallationId(UUID installationId);

    @Modifying
    @Query("""
        WITH to_update AS (
            SELECT ci.contract_item_id
            FROM contract_item ci
            WHERE ci.contract_item_id = :contractItemId

            UNION ALL

            SELECT ci.contract_item_id
            FROM contract_item ci
            JOIN contract_reference_item cri
                ON cri.contract_reference_item_id = ci.contract_item_reference_id
            JOIN pre_measurement_street_item si
                ON si.contract_item_id = ci.contract_item_id
            WHERE lower(cri.item_dependency) = :dependency
              AND lower(cri.type) IN ('projeto', 'serviço')
              AND si.pre_measurement_id = :preMeasurementId
        )
        UPDATE contract_item ci
        SET quantity_executed = quantity_executed + :quantityExecuted
        FROM to_update tu
        WHERE ci.contract_item_id = tu.contract_item_id
        """)
    void updateExecutedQuantity(
            @Param("contractItemId") Long contractItemId,
            @Param("dependency") String dependency,
            @Param("preMeasurementId") Long preMeasurementId,
            @Param("quantityExecuted") BigDecimal quantityExecuted
    );

    @Modifying
    @Query(
            """
                UPDATE contract_item
                SET quantity_executed = quantity_executed + :quantityExecuted
                WHERE contract_item_id = :contractItemId
            """
    )
    void updateExecutedQuantity(
            @Param("contractItemId") Long contractItemId,
            @Param("quantityExecuted") BigDecimal quantityExecuted
    );

    @Modifying
    @Query(
            """
                UPDATE pre_measurement_street
                SET execution_photo_uri = :fileUri, street_status = :streetStatus
                WHERE device_pre_measurement_street_id = :streetId
            """
    )
    void saveInstallationStreetPhotoUri(
            String fileUri,
            String streetStatus,
            UUID streetId
    );

    @Modifying
    @Query(
            """
                UPDATE pre_measurement
                SET signature_uri = :fileUri,
                    sign_date = :signDate,
                    responsible = :responsible,
                    status = :installationStatus
                WHERE pre_measurement_id = :installationId
            """
    )
    void saveInstallationSignPhotoUri(
            String fileUri,
            Instant signDate,
            String responsible,
            String installationStatus,
            Long installationId
    );


}
