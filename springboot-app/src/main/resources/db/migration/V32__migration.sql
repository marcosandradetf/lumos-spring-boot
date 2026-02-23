alter table direct_execution_street
    add column if not exists point_number int;

alter table pre_measurement_street
    add column if not exists point_number int;

alter table maintenance_street
    add column if not exists point_number int;

-- INSTALLATION STREET
CREATE OR REPLACE VIEW installation_street_view AS
SELECT des.direct_execution_street_id AS installation_street_id,
       des.direct_execution_id        AS installation_id,
       'DIRECT_EXECUTION'             AS installation_type,
       des.address,
       des.last_power,
       des.latitude,
       des.longitude,
       des.current_supply,
       des.finished_at,
       des.street_status,
       des.device_id,
       des.execution_photo_uri,
       des.point_number
FROM direct_execution_street des
UNION ALL
SELECT pms.pre_measurement_street_id AS installation_street_id,
       pms.pre_measurement_id        AS installation_id,
       'PRE_MEASUREMENT'             AS installation_type,
       pms.address,
       pms.last_power,
       pms.installation_latitude as latitude,
       pms.installation_longitude as longitude,
       pms.current_supply,
       pms.finished_at,
       pms.street_status,
       pms.device_id,
       pms.execution_photo_uri,
       pms.point_number
FROM pre_measurement_street pms;