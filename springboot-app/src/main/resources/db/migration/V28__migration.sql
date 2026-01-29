alter table direct_execution
    add column if not exists available_at timestamptz;

alter table pre_measurement
    add column if not exists available_at timestamptz;

alter table direct_execution
    add column if not exists finished_at timestamptz;

alter table pre_measurement
    add column if not exists finished_at timestamptz;

alter table maintenance
    add column if not exists finished_at timestamptz;

UPDATE direct_execution
SET available_at   = assigned_at + INTERVAL '30 minutes',
    finished_at    = assigned_at + INTERVAL '4 hours',
    report_view_at = assigned_at + INTERVAL '6 hours'
WHERE available_at IS NULL;


update maintenance
set finished_at    = date_of_visit + INTERVAL '4 hours',
    report_view_at = date_of_visit + INTERVAL '6 hours'
where report_view_at is null;


CREATE INDEX IF NOT EXISTS idx_pre_measurement_status_available
    ON pre_measurement (tenant_id, status, available_at);

CREATE INDEX IF NOT EXISTS idx_pre_measurement_finished_at
    ON pre_measurement (tenant_id, finished_at);

CREATE INDEX IF NOT EXISTS idx_pre_measurement_report_view_at
    ON pre_measurement (tenant_id, report_view_at);


CREATE INDEX IF NOT EXISTS idx_direct_execution_status_available
    ON direct_execution (tenant_id, direct_execution_status, available_at);

CREATE INDEX IF NOT EXISTS idx_direct_execution_finished_at
    ON direct_execution (tenant_id, finished_at);

CREATE INDEX IF NOT EXISTS idx_direct_execution_report_view_at
    ON direct_execution (tenant_id, report_view_at);


CREATE INDEX IF NOT EXISTS idx_maintenance_finished_at
    ON maintenance (tenant_id, finished_at);

CREATE INDEX IF NOT EXISTS idx_maintenance_report_view_at
    ON maintenance (tenant_id, report_view_at);
