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

DO $$
    BEGIN
        -- Verifica se a coluna 'steps' existe na tabela 'pre_measurement'
        IF EXISTS (SELECT 1
                   FROM information_schema.columns
                   WHERE table_name = 'pre_measurement'
                     AND column_name = 'street') THEN
            -- Se existir, renomeia a coluna 'steps' para 'step'
            ALTER TABLE pre_measurement RENAME COLUMN street TO address;
        END IF;
    END $$;


DO $$
    BEGIN
        -- Verifica se a coluna 'steps' existe na tabela 'pre_measurement'
        IF EXISTS (SELECT 1
                   FROM information_schema.columns
                   WHERE table_name = 'pre_measurement'
                     AND column_name = 'steps') THEN
            -- Se existir, renomeia a coluna 'steps' para 'step'
            ALTER TABLE pre_measurement RENAME COLUMN steps TO step;
        END IF;
    END $$;


ALTER TABLE pre_measurement_street_item
    ALTER COLUMN measured_item_quantity TYPE NUMERIC;

alter table pre_measurement
    add column if not exists created_by_user_id uuid;

alter table pre_measurement
    add column if not exists created_at timestamptz not null default now();