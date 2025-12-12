alter table direct_execution
    ADD COLUMN IF NOT EXISTS signature_uri TEXT,
    ADD COLUMN IF NOT EXISTS responsible TEXT,
    ADD COLUMN IF NOT EXISTS sign_date timestamp;

alter table pre_measurement_street
    add column if not exists current_supply text,
    add column if not exists installation_latitude DOUBLE PRECISION,
    add column if not exists installation_longitude DOUBLE PRECISION;

alter table pre_measurement_street_item
    add column if not exists quantity_executed numeric not null default 0;