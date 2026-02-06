ALTER TABLE direct_execution
    ADD COLUMN IF NOT EXISTS started_at TIMESTAMPTZ;

ALTER TABLE pre_measurement
    ADD COLUMN IF NOT EXISTS started_at TIMESTAMPTZ;

UPDATE direct_execution de
SET finished_at = (SELECT MAX(des.finished_at)
                   FROM direct_execution_street des
                   WHERE des.direct_execution_id = de.direct_execution_id)
WHERE EXISTS (SELECT 1
              FROM direct_execution_street des
              WHERE des.direct_execution_id = de.direct_execution_id)
  and direct_execution_status = 'FINISHED';

update direct_execution
set started_at = available_at + interval '45 minutes'
where direct_execution_status = 'FINISHED';

update direct_execution
set report_view_at = finished_at + interval '30 minutes'
where direct_execution_status = 'FINISHED';

update maintenance
set finished_at = coalesce(sign_date, date_of_visit + interval '4 hours')
where status = 'FINISHED';

update maintenance
set report_view_at = finished_at + interval '30 minutes'
where status = 'FINISHED';

-- INSTALLATION
CREATE OR REPLACE VIEW installation_view AS
SELECT de.direct_execution_id AS installation_id,
       'DIRECT_EXECUTION'     AS installation_type,
       de.contract_id,
       de.team_id,
       de.assigned_user_id,
       de.description,
       de.step,
       de.signature_uri,
       de.responsible,
       de.sign_date,
       de.report_view_at,
       de.available_at,
       de.finished_at,
       de.tenant_id,
       de.started_at,
       de.direct_execution_status as status
FROM direct_execution de

UNION ALL

SELECT pm.pre_measurement_id   AS installation_id,
       'PRE_MEASUREMENT'       AS installation_type,
       pm.contract_contract_id AS contract_id,
       pm.team_id,
       pm.assign_by_user_id    AS assigned_user_id,
       pm.comment              AS description,
       pm.step,
       pm.signature_uri,
       pm.responsible,
       pm.sign_date,
       pm.report_view_at,
       pm.available_at,
       pm.finished_at,
       pm.tenant_id,
       pm.started_at,
       pm.status
FROM pre_measurement pm;


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
       des.execution_photo_uri
FROM direct_execution_street des

UNION ALL

SELECT pms.pre_measurement_street_id AS installation_street_id,
       pms.pre_measurement_id        AS installation_id,
       'PRE_MEASUREMENT'             AS installation_type,
       pms.address,
       pms.last_power,
       pms.latitude,
       pms.longitude,
       pms.current_supply,
       pms.finished_at,
       pms.street_status,
       pms.device_id,
       pms.execution_photo_uri
FROM pre_measurement_street pms;

-- INSTALLATION ITEM
CREATE OR REPLACE VIEW installation_street_item_view AS
SELECT desi.direct_execution_street_item_id AS installation_street_item_id,
       desi.direct_execution_street_id      AS installation_street_id,
       'DIRECT_EXECUTION'                   AS installation_type,
       desi.contract_item_id,
       desi.executed_quantity               AS executed_quantity
FROM direct_execution_street_item desi

UNION ALL

SELECT pmsi.pre_measurement_street_item_id AS installation_street_item_id,
       pmsi.pre_measurement_street_id      AS installation_street_id,
       'PRE_MEASUREMENT'                   AS installation_type,
       pmsi.contract_item_id,
       pmsi.quantity_executed              AS executed_quantity
FROM pre_measurement_street_item pmsi;

-- INSTALLATION EXECUTOR
CREATE OR REPLACE VIEW installation_executor_view AS
SELECT de.direct_execution_id AS installation_id,
       'DIRECT_EXECUTION'     AS installation_type,
       de.user_id
FROM direct_execution_executor de

UNION ALL

SELECT pe.pre_measurement_id AS installation_id,
       'PRE_MEASUREMENT'     AS installation_type,
       pe.user_id
FROM pre_measurement_executor pe;

----INDEX
----
-- filtro principal por tenant + contrato + período
CREATE INDEX IF NOT EXISTS idx_de_tenant_contract_finished
    ON direct_execution (tenant_id, contract_id, finished_at);

-- quando buscar apenas por tenant + período
CREATE INDEX IF NOT EXISTS idx_de_tenant_finished
    ON direct_execution (tenant_id, finished_at);

-- join e navegação
CREATE INDEX IF NOT EXISTS idx_de_team
    ON direct_execution (team_id);

CREATE INDEX IF NOT EXISTS idx_de_assigned_user
    ON direct_execution (assigned_user_id);

CREATE INDEX IF NOT EXISTS idx_pm_tenant_contract_finished
    ON pre_measurement (tenant_id, contract_contract_id, finished_at);

CREATE INDEX IF NOT EXISTS idx_pm_tenant_finished
    ON pre_measurement (tenant_id, finished_at);

CREATE INDEX IF NOT EXISTS idx_pm_team
    ON pre_measurement (team_id);

CREATE INDEX IF NOT EXISTS idx_pm_assigned_user
    ON pre_measurement (assign_by_user_id);

CREATE INDEX IF NOT EXISTS idx_pms_measurement
    ON pre_measurement_street (pre_measurement_id);

CREATE INDEX IF NOT EXISTS idx_pms_measurement_finished
    ON pre_measurement_street (pre_measurement_id, finished_at);

CREATE INDEX IF NOT EXISTS idx_pms_status
    ON pre_measurement_street (street_status);

CREATE INDEX IF NOT EXISTS idx_desi_street
    ON direct_execution_street_item (direct_execution_street_id);

CREATE INDEX IF NOT EXISTS idx_desi_contract_item
    ON direct_execution_street_item (contract_item_id);

CREATE INDEX IF NOT EXISTS idx_pmsi_street
    ON pre_measurement_street_item (pre_measurement_street_id);

CREATE INDEX IF NOT EXISTS idx_pmsi_contract_item
    ON pre_measurement_street_item (contract_item_id);

-- queries costumam filtrar por installation + user
CREATE INDEX IF NOT EXISTS idx_de_executor_execution_user
    ON direct_execution_executor (direct_execution_id, user_id);

CREATE INDEX IF NOT EXISTS idx_pm_executor_measurement_user
    ON pre_measurement_executor (pre_measurement_id, user_id);
