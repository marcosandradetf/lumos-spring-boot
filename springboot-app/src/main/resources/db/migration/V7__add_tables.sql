ALTER TABLE tb_contracts_items
    ADD COLUMN IF NOT EXISTS quantity_executed float8 NULL;

ALTER TABLE tb_pre_measurements_streets
    ADD COLUMN IF NOT EXISTS photo_uri varchar(255) NULL;

ALTER TABLE tb_pre_measurements_streets
    ALTER COLUMN latitude DROP NOT NULL,
    ALTER COLUMN longitude DROP NOT NULL;

update tb_pre_measurements_streets set neighborhood = null, number = null, latitude = null, longitude = null, last_power = null, comment = null;