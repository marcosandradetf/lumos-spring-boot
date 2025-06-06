ALTER TABLE tb_contracts_items
    ADD COLUMN IF NOT EXISTS quantity_executed float8 NOT NULL DEFAULT(0.0);

ALTER TABLE tb_pre_measurements_streets
    ADD COLUMN IF NOT EXISTS photo_uri varchar(255) NULL,
    ADD COLUMN IF NOT EXISTS device_street_id bigint NULL,
    ADD COLUMN IF NOT EXISTS device_id VARCHAR(36) null;

ALTER TABLE tb_pre_measurements_streets
    ALTER COLUMN latitude DROP NOT NULL,
    ALTER COLUMN longitude DROP NOT NULL;

UPDATE tb_pre_measurements_streets set neighborhood = null, number = null, latitude = null, longitude = null, last_power = null, comment = null;

UPDATE tb_Contracts SET status = 'ACTIVE';