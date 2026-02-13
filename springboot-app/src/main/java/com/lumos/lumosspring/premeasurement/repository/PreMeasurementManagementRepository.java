package com.lumos.lumosspring.premeasurement.repository;

import com.lumos.lumosspring.util.ExecutionStatus;
import com.lumos.lumosspring.util.Utils;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class PreMeasurementManagementRepository {
    private final NamedParameterJdbcTemplate namedJDBC;

    public PreMeasurementManagementRepository(NamedParameterJdbcTemplate namedJDBC) {
        this.namedJDBC = namedJDBC;

    }

    public void delegatePreMeasurementToExecution(
            Long preMeasurementID,
            Long teamID,
            Long managementID,
            String comment,
            List<Long> priorityStreets
    ) {
        var userID = Utils.INSTANCE.getCurrentUserId();

        namedJDBC.update(
                """
                            update pre_measurement
                            set team_id = :teamID, comment = :comment, reservation_management_id = :managementID, status = :status,
                            assign_by_user_id = :userID
                            where pre_measurement_id = :preMeasurementID
                        """,
                Map.of(
                        "preMeasurementID", preMeasurementID,
                        "teamID", teamID,
                        "managementID", managementID,
                        "comment", comment,
                        "status", ExecutionStatus.WAITING_STOCKIST,
                        "userID", userID
                )
        );

        if (!priorityStreets.isEmpty()) {
            namedJDBC.update(
                    """
                                update pre_measurement_street
                                set prioritized = true
                                where pre_measurement_street_id in (:priorityStreets)
                            """,
                    Map.of(
                            "priorityStreets", priorityStreets
                    )
            );
        }
    }

    public void markAsAvailable(Long preMeasurementId) {
        namedJDBC.update(
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
