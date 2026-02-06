package com.lumos.lumosspring.premeasurement.repository.installation;

import com.lumos.lumosspring.premeasurement.model.PreMeasurement;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PreMeasurementInstallationRepository extends CrudRepository<PreMeasurement, Long> {
    @Modifying
    @Query(
        """
            update pre_measurement
            set report_view_at = now()
            where pre_measurement_id in (:ids) and report_view_at is null
        """
    )
    void registerGeneration(List<Long> ids);

    record PreMeasurementRow(Long preMeasurementId, String status, String description, String preMeasurementPhotoUri, Long preMeasurementStreetId) {}

    @Query(
            """
                SELECT p.pre_measurement_id, p.status, 'Etapa ' || p.step || ' - ' || p.city as description, s.pre_measurement_photo_uri, s.pre_measurement_street_id
                FROM pre_measurement p
                JOIN pre_measurement_street s on s.pre_measurement_id = p.pre_measurement_id
                WHERE s.device_pre_measurement_street_id = :streetId
            """
    )
    PreMeasurementRow getInstallationByDeviceStreetId(UUID streetId);

    @Query(
            """
                SELECT p.pre_measurement_id, p.status, 'Etapa ' || p.step || ' - ' || p.city as description, null, null
                FROM pre_measurement p
                where p.device_pre_measurement_id = :installationId
            """
    )
    PreMeasurementRow getInstallationByDeviceInstallationId(UUID installationId);

    @Modifying
    @Query(
            """
                UPDATE pre_measurement_street
                SET execution_photo_uri = :fileUri,
                    street_status = :streetStatus,
                    current_supply = :currentSupply,
                    last_power = :lastPower,
                    installation_latitude = :latitude,
                    installation_longitude = :longitude
                WHERE device_pre_measurement_street_id = :streetId
            """
    )
    void finishInstallationStreet(
            String fileUri,
            String streetStatus,
            UUID streetId,
            String currentSupply,
            String lastPower,
            Double latitude,
            Double longitude
    );

    @Modifying
    @Query(
            """
                UPDATE pre_measurement_street_item
                SET quantity_executed = :quantityExecuted
                WHERE contract_item_id = :contractItemId AND pre_measurement_street_id = :preMeasurementStreetId
            """
    )
    void updateInstallationItem(Long contractItemId, BigDecimal quantityExecuted, Long preMeasurementStreetId);

    @Modifying
    @Query(
            """
                UPDATE pre_measurement
                SET signature_uri = :fileUri,
                    sign_date = :signDate,
                    responsible = :responsible,
                    status = :installationStatus,
                    finished_at = :finishedAt,
                    started_at = COALESCE(:startedAt, available_at + interval '45 minutes')
                WHERE pre_measurement_id = :installationId;
            """
    )
    void saveInstallationSignPhotoUri(
            String fileUri,
            Instant signDate,
            String responsible,
            String installationStatus,
            Long installationId,
            Instant finishedAt,
            Instant startedAt
    );
}
