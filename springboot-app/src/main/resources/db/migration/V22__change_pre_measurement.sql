alter table pre_measurement
    add column if not exists device_pre_measurement_id uuid;

alter table pre_measurement_street
    drop column if exists deviceId;

alter table pre_measurement_street
    drop column if exists deviceStreetId;

alter table pre_measurement_street
    drop column if exists number;

alter table pre_measurement_street
    drop column if exists neighborhood;

alter table pre_measurement_street
    drop column if exists state;

alter table pre_measurement_street
    drop column if exists city;

alter table pre_measurement_street
    add column if not exists device_pre_measurement_street_id uuid;

alter table pre_measurement_street
    rename street to address;

alter table pre_measurement
    rename steps to step;

ALTER TABLE pre_measurement_street_item
    ALTER COLUMN measured_item_quantity TYPE NUMERIC;