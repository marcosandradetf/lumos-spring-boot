alter table direct_execution_item
    add column item_status TEXT not null default 'PENDING';

ALTER TABLE pre_measurement_street_item
    ALTER COLUMN item_status SET NOT NULL,
    ALTER COLUMN item_status SET DEFAULT 'PENDING';