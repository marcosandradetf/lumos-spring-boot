package com.lumos.lumosspring.premeasurement.repository.report;

import com.lumos.lumosspring.premeasurement.model.PreMeasurement;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PreMeasurementReportRepository extends CrudRepository<PreMeasurement, Long> {
    @Query(
            """
                WITH items_by_street AS (
                    SELECT
                        pesi.pre_measurement_street_id,
                        ci.contract_item_id,
                        ci.unit_price,
                        pesi.quantity_executed,
                        cri.description,
                        coalesce(cri.name_for_import, cri.description) AS name_for_import
                    FROM pre_measurement_street_item pesi
                    JOIN contract_item ci
                        ON ci.contract_item_id = pesi.contract_item_id
                    JOIN contract_reference_item cri
                        ON cri.contract_reference_item_id = ci.contract_item_reference_id
                    WHERE pesi.pre_measurement_id = :preMeasurementId
                    ORDER BY cri.description
                ),
                items_by_street_distinct AS (
                    SELECT DISTINCT contract_item_id, description
                    FROM items_by_street
                    ORDER BY description
                )
                SELECT json_build_object(
                    'company', json_build_object(
                        'social_reason', com.social_reason,
                        'company_cnpj', com.company_cnpj,
                        'company_address', com.company_address,
                        'company_phone', coalesce(com.company_phone, ''),
                        'company_logo', com.company_logo
                    ),
                    'contract', json_build_object(
                        'contract_number', c.contract_number,
                        'contractor', c.contractor,
                        'cnpj', c.cnpj,
                        'address', c.address,
                        'phone', coalesce(c.phone, '')
                    ),
                    'values', (
                        SELECT json_agg(
                            json_build_object(
                                'description', agg.description,
                                'unit_price', agg.unit_price,
                                'total_price', ROUND(agg.total_quantity * agg.unit_price, 2),
                                'quantity_executed', agg.total_quantity
                            )
                        )
                        FROM (
                            SELECT
                                description,
                                unit_price,
                                SUM(quantity_executed) AS total_quantity
                            FROM items_by_street
                            GROUP BY description, unit_price
                            ORDER BY description
                        ) AS agg
                    ),
                    'columns', (
                        SELECT to_json(
                            ARRAY[
                                'ENDEREÇO',
                                'P.A'
                            ] || (
                                SELECT array_agg(DISTINCT cci.name_for_import)
                                FROM items_by_street cci
                            ) || ARRAY[
                                'DATA',
                                'FORNECEDOR'
                            ]
                        )
                    ),
                    'team', execs.executors,
                    'streets', (
                        SELECT json_agg(street_row)
                        FROM (
                            SELECT json_build_array(
                                pes.address,
                                coalesce(pes.last_power, ''),
                                (
                                    SELECT json_agg(
                                        CASE WHEN EXISTS (
                                            SELECT 1
                                            FROM items_by_street ibs
                                            WHERE ibs.pre_measurement_street_id = pes.pre_measurement_street_id
                                            AND ibs.contract_item_id = ci.contract_item_id
                                        ) THEN (
                                            SELECT SUM(ibs.quantity_executed)
                                            FROM items_by_street ibs
                                            WHERE ibs.pre_measurement_street_id = pes.pre_measurement_street_id
                                            AND ibs.contract_item_id = ci.contract_item_id
                                        ) ELSE 0 END
                                    )
                                    FROM items_by_street_distinct ci
                                ),
                                pes.finished_at,
                                coalesce(pes.current_supply, '')
                            ) AS street_row
                            FROM pre_measurement_street pes
                            WHERE pes.pre_measurement_id = :preMeasurementId
                            ORDER BY pes.finished_at
                        ) AS ordered_rows
                    ),
                    'street_sums', (
                        SELECT json_agg(total_sum)
                        FROM (
                            SELECT SUM(quantity_executed) AS total_sum
                            FROM items_by_street
                            GROUP BY contract_item_id, description
                            ORDER BY description
                        ) summed
                    ),
                    'total', (
                        SELECT json_build_object(
                            'total_price', ROUND(SUM(quantity_executed * unit_price), 2)
                        )
                        FROM items_by_street
                    )
                ) AS result
                FROM pre_measurement p
                JOIN contract c ON c.contract_id = p.contract_contract_id
                JOIN company com ON com.id_company = c.company_id
                LEFT JOIN LATERAL (
                    SELECT json_agg(
                        json_build_object(
                            'name', t.name,
                            'last_name', t.last_name,
                            'role', t.role_name
                        )
                    ) AS executors
                    FROM (
                        SELECT DISTINCT ON (au.user_id)
                               au.name,
                               au.last_name,
                               r.role_name
                        FROM pre_measurement_executor pee
                        JOIN app_user au ON au.user_id = pee.user_id
                        JOIN user_role ur ON ur.id_user = au.user_id
                        JOIN role r ON r.role_id = ur.id_role
                        WHERE pee.pre_measurement_id = p.pre_measurement_id
                        ORDER BY au.user_id, r.role_name
                    ) t
                ) execs ON TRUE
                WHERE p.pre_measurement_id = :preMeasurementId;
            """
    )
    String getDataForReport(Long preMeasurementId);
}